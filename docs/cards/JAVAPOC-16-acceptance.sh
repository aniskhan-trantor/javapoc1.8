#!/usr/bin/env bash
# Acceptance tests for JAVAPOC-16 (README Java 17 section). One check per criterion, named from its number.
# Run from anywhere: bash docs/cards/JAVAPOC-16-acceptance.sh (it checks the repository it lives in).
# Exit 0 when every check passes, 1 when any fails, 2 when the script cannot run.
set -u

root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel 2>/dev/null) || { echo "ERROR: not inside a git repository"; exit 2; }
readme="$root/README.md"
[ -f "$readme" ] || { echo "ERROR: $readme not found"; exit 2; }

# README text without carriage returns, so the checks work on CRLF and LF checkouts.
text=$(tr -d '\r' < "$readme")
# Section 13: from its heading to the next "## " heading or the end of the file.
section=$(printf '%s\n' "$text" | awk '/^## 13\. Java 17[[:space:]]*$/{on=1; print; next} on && /^## /{exit} on{print}')
# Section 7: from its heading to the next "## " heading.
section7=$(printf '%s\n' "$text" | awk '/^## 7\. Build and test[[:space:]]*$/{on=1; next} on && /^## /{exit} on{print}')

failures=0
pass() { echo "PASS $1"; }
fail() { echo "FAIL $1: $2"; failures=$((failures + 1)); }

# has <haystack> <fixed string>
has() { printf '%s\n' "$1" | grep -qF -- "$2"; }

# need <check name> <haystack label> <haystack> <fixed string>... : fail on the first missing string
need() {
  local name=$1 label=$2 hay=$3; shift 3
  local missing=()
  for s in "$@"; do has "$hay" "$s" || missing+=("'$s'"); done
  if [ ${#missing[@]} -eq 0 ]; then return 0; fi
  fail "$name" "$label lacks ${missing[*]}"
  return 1
}

AC1_section_listed_and_scope() {
  local n=AC1_section_listed_and_scope
  [ -n "$section" ] || { fail $n "no '## 13. Java 17' heading"; return; }
  has "$text" "13. [Java 17](#13-java-17)" || { fail $n "Contents lacks '13. [Java 17](#13-java-17)'"; return; }
  if ! has "$section" "JDK 11 and 17" && ! { has "$section" "JDK 11" && has "$section" "JDK 17"; }; then
    fail $n "section names neither 'JDK 11 and 17' nor both 'JDK 11' and 'JDK 17'"; return
  fi
  need $n "section 13" "$section" '`common`' '`workflow-app`' '--release 8' 'java-compat' || return
  pass $n
}

AC2_local_command() {
  local n=AC2_local_command
  need $n "section 13" "$section" 'mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8' || return
  pass $n
}

AC3_spark_job_and_cluster() {
  local n=AC3_spark_job_and_cluster
  need $n "section 13" "$section" '`spark-job`' 'Spark 2.4' 'Scala 2.11' 'EMR 5.36' 'Java 8' || return
  pass $n
}

AC4_changes_and_link() {
  local n=AC4_changes_and_link
  need $n "section 13" "$section" 'Spring 5.3' 'H2 2.' '](#9-configuration)' || return
  pass $n
}

AC5_only_readme() {
  local n=AC5_only_readme
  git -C "$root" fetch -q origin main 2>/dev/null || { fail $n "cannot fetch origin main"; return; }
  local base files bad
  base=$(git -C "$root" merge-base origin/main HEAD) || { fail $n "no merge base with origin/main"; return; }
  files=$(git -C "$root" diff --name-only "$base" HEAD)
  bad=$(printf '%s\n' "$files" | grep -v -e '^README\.md$' -e '^docs/cards/JAVAPOC-16' -e '^$')
  [ -z "$bad" ] || { fail $n "files other than README.md and the card documents changed: $(echo $bad)"; return; }
  pass $n
}

# Not numbered criteria: the person's answer to JAVAPOC-Q6, item 2 (fix the two stale facts).
Q6a_spring_version() {
  local n=Q6a_spring_version
  has "$text" "Spring MVC 4.3" && { fail $n "README still says 'Spring MVC 4.3'"; return; }
  printf '%s\n' "$text" | grep -F '| API and workflow |' | grep -qF 'Spring MVC 5.3' \
    || { fail $n "the 'API and workflow' row lacks 'Spring MVC 5.3'"; return; }
  pass $n
}

Q6b_backend_test_count() {
  local n=Q6b_backend_test_count
  local row
  row=$(printf '%s\n' "$section7" | grep -F 'spark-job' | grep -F '`mvn clean test`')
  [ -n "$row" ] || { fail $n "section 7 has no backend row with 'mvn clean test'"; return; }
  printf '%s\n' "$row" | grep -qE '\|[[:space:]]*69[[:space:]]*\|[[:space:]]*$' \
    || { fail $n "backend row does not end '| 69 |': $row"; return; }
  printf '%s\n' "$section7" | grep -qE '\|[[:space:]]*67[[:space:]]*\|' && { fail $n "section 7 still has a '| 67 |' cell"; return; }
  pass $n
}

AC1_section_listed_and_scope
AC2_local_command
AC3_spark_job_and_cluster
AC4_changes_and_link
AC5_only_readme
Q6a_spring_version
Q6b_backend_test_count

echo "failures: $failures"
[ "$failures" -eq 0 ]
