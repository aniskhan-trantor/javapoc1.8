# AGENTS.md

Instructions for AI agents (and a short orientation for people) working in this repository. The README
explains how to run the system; this file says how to change it safely.

## What this is

Sales Batch: upload a sales CSV, load it into HBase 1.4, aggregate it with a Spark 2.4 (Scala 2.11) job, wait
for a manager's approval, then write `report-<businessDate>.csv`. Design: `latest-ARCHITECTURE.md`.

| Module | What | Notes |
|---|---|---|
| `common` | HBase repository and shared records | HBase tests use Mockito, not a real HBase |
| `spark-job` | Scala Spark job (`SalesAggregator`) | Runs on the cluster (EMR 5.36, Java 8); bundles the HBase client |
| `workflow-app` | Spring MVC 5.3, Activiti 5.22 and embedded Jetty API | Listens on `127.0.0.1` by default |
| `ui` | Angular 20 | Talks to the API through a proxy |

## Build and test

```sh
mvn -B test -pl common,spark-job,workflow-app    # what CI runs (Java 8); 30 + 7 (2 skipped) + 29 tests
cd ui && npm ci && npm run build                  # the UI
```

CI (`.github/workflows/ci.yml`): the required check is `test` (JDK 8). `java-compat` runs `common` and
`workflow-app` on JDK 11 and 17 (`-Dmaven.compiler.release=8`); `spark-job` cannot run on JDK 11 or 17 until
Spark 2.4 and Scala 2.11 are upgraded, so it has its own non-blocking job.

The test `BatchControllerTest.approvingTwiceIsAConflict` is known to fail now and then ("No pending approval"):
a race between the run's status and its approval task. Rerun before assuming your change broke it, and do not
"fix" it by editing the test.

## Rules for changes

- Work on a branch (one per card, `card/<KEY>`) and open a pull request. Never push to `main`; a person merges.
- Do not change existing tests to make a change pass; add new tests beside them.
- Keep the code compatible with Java 8 and Scala 2.11 (the cluster runs them). Java 17 is the migration
  target: see the report in the programme repository (`.prime/readiness/java17-upgrade-chain.md`).
- Do not touch `docker/`, the EMR and Spark runtime versions, or `sample-data/` without a person's decision.
- `log4j` 1.2 is banned from `workflow-app` (an Enforcer rule fails the build); `spark-job` keeps it because
  Spark 2.4 needs it.
- Never commit secrets or tokens. Approver tokens are configuration (`-Dapprovers` or `APPROVERS`), not code.

## Security-relevant settings

`server.host` (default `127.0.0.1`), `approvers` (`name:token,...`, tokens of 16 or more characters),
`batch.input.dir` (folder `POST /batches` may read) and `upload.max.bytes`. See README section 9. Without
approvers configured, the approve endpoint is open and the server refuses any address except the loopback one.
The read endpoints (`GET /batches/{id}`, `/data/*`) are not authenticated.

## How to check your own work

1. `mvn -B test -pl common,spark-job,workflow-app` is green (rerun once if only the known flaky test fails).
2. The pull request's `test` and `java-compat` checks are green.
3. You changed only what the card says; the diff shows no unrelated files (`target/` folders are not tracked).
