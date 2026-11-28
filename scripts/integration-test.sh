#!/usr/bin/env bash
# Integration test of the real batch path: a real HBase (docker/local) with the repository tests and the real
# Spark job (SalesAggregator, local[2]). It is the safety net for the HBase, Hadoop, Spark and Scala upgrades.
#
#   bash scripts/integration-test.sh            start HBase if it is not running, run the tests, stop only an HBase it started
#   KEEP_HBASE=1 bash scripts/integration-test.sh   leave HBase running afterwards
#
# Needs: Docker with the compose plugin, Maven, JDK 8, and the name host.docker.internal resolving to 127.0.0.1
# (HBase advertises that name; Docker Desktop and Rancher Desktop add it. On plain Linux:
#   echo "127.0.0.1 host.docker.internal" | sudo tee -a /etc/hosts ).
# Exit 0 only when every integration test ran and passed. A skipped test is a failure here: without this check
# the tests silently skip when no HBase answers and the job would look green while proving nothing.
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker/local/docker-compose.yml)
CLASSES=(com.example.salesbatch.common.HBaseSalesRepositoryRealTest com.example.salesbatch.spark.SalesAggregatorHBaseTest)
MODULES=(common spark-job)

# Only stop what this script started: a developer's own local stack (HBase, Spark) may already be running, and its
# data must never be touched. Never "down -v": that would delete the stack's volumes.
started_here=0
cleanup() {
  if [ "$started_here" = "1" ] && [ "${KEEP_HBASE:-0}" != "1" ]; then
    "${COMPOSE[@]}" rm -sf hbase >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if docker ps --format '{{.Names}}' | grep -qx sales-hbase; then
  echo "== using the HBase that is already running (sales-hbase); it is left running"
else
  echo "== starting HBase"
  "${COMPOSE[@]}" up -d hbase
  started_here=1
fi

echo "== waiting for ZooKeeper (2181), the HBase master (16000) and the region server (16020)"
deadline=$((SECONDS + 240))
while true; do
  ready=1
  for port in 2181 16000 16020; do
    (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null || ready=0
  done
  [ "$ready" = "1" ] && break
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "HBase did not come up in time. Last container log lines:" >&2
    docker logs --tail 30 sales-hbase >&2 || true
    exit 1
  fi
  sleep 3
done
sleep 10 # the ports open before the master has finished initialising

# HBase hands clients the name host.docker.internal: it must resolve here, or every client call times out.
if ! (exec 3<>/dev/tcp/host.docker.internal/16020) 2>/dev/null; then
  echo "host.docker.internal does not resolve to this machine. Add 127.0.0.1 host.docker.internal to your hosts file." >&2
  exit 2
fi

echo "== running the integration tests"
mvn -B test -pl "$(IFS=,; echo "${MODULES[*]}")" \
  -Dtest="$(IFS=,; echo "${CLASSES[*]}")" -DfailIfNoTests=false

echo "== checking that nothing was skipped"
status=0
for class in "${CLASSES[@]}"; do
  report=$(ls */target/surefire-reports/TEST-"$class".xml 2>/dev/null | head -1 || true)
  if [ -z "$report" ]; then
    echo "NO REPORT for $class" >&2; status=1; continue
  fi
  tests=$(grep -o -m1 ' tests="[0-9]*"' "$report" | grep -o '[0-9]*')
  skipped=$(grep -o -m1 ' skipped="[0-9]*"' "$report" | grep -o '[0-9]*')
  echo "$class: $tests tests, $skipped skipped"
  if [ "${tests:-0}" -eq 0 ] || [ "${skipped:-1}" -ne 0 ]; then
    echo "SKIPPED or EMPTY: $class did not really run" >&2; status=1
  fi
done
exit $status
