#!/usr/bin/env bash
# Verify's own single-fault mutants of README.md (independent of the builder's recipe).
# Each mutant is applied to a fresh copy of the PR-head clone; the script must fail on the named check.
set -u
here=${1:?usage: bash JAVAPOC-16-verify-mutants.sh FOLDER  (FOLDER holds a clone of the PR head named head)}
src="$here/head"
work="$here/sens"
out="$here/verify-mutants.txt"
: > "$out"

run() { # run <expected check> <description> <sed expression or "COMMIT:<file>">
  local want=$1 desc=$2 expr=$3
  rm -rf "$work"; cp -r "$src" "$work"
  if [[ $expr == COMMIT:* ]]; then
    local f=${expr#COMMIT:}
    echo "# mutant" >> "$work/$f"
    git -C "$work" -c user.name=v -c user.email=v@v commit -qam "mutant" >/dev/null
  else
    sed -i -E "$expr" "$work/README.md"
    if git -C "$work" diff --quiet -- README.md; then echo "NOT-APPLIED | $desc" >> "$out"; return; fi
  fi
  local res
  res=$(bash "$work/docs/cards/JAVAPOC-16-acceptance.sh" 2>&1)
  local fails
  fails=$(printf '%s\n' "$res" | grep '^FAIL' | cut -d: -f1 | sed 's/^FAIL //' | tr '\n' ' ')
  if printf '%s\n' "$res" | grep -q "^FAIL $want"; then v=KILLED; elif [ -z "$fails" ]; then v=SURVIVED; else v=FAILED-OTHER; fi
  echo "$v | $desc | failed: ${fails:-none}" >> "$out"
}

run AC1 "Contents line points to a wrong anchor"        's/\(#13-java-17\)/(#13-java17)/'
run AC1 "JDK 11 and 17 replaced by JDK 11 only"          's/JDK 11 and 17/JDK 11/g'
run AC1 "--release 8 changed to --release 11"           's/--release 8/--release 11/g'
run AC1 "java-compat job name removed"                  's/the `java-compat` pipeline job/the pipeline/g'
run AC1 "workflow-app row removed"                      '/^\| `workflow-app` \| Yes/d'
run AC2 "local command loses -Dmaven.compiler.release"  's/^mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8$/mvn -B test -pl common,workflow-app/'
run AC2 "local command adds spark-job"                  's/^mvn -B test -pl common,workflow-app/mvn -B test -pl common,spark-job,workflow-app/'
run AC3 "Scala 2.11 changed to Scala 2.12"              '/^`spark-job` uses/s/Scala 2.11/Scala 2.12/'
run AC3 "EMR 5.36 changed to EMR 6.15"                  '/^`spark-job` uses/s/EMR 5.36/EMR 6.15/'
run AC3 "cluster Java 8 changed to Java 11"             '/^`spark-job` uses/s/Java 8/Java 11/g'
run AC3 "Spark 2.4 changed to Spark 3.3"                '/^`spark-job` uses/s/Spark 2.4/Spark 3.3/g'
run AC4 "Spring 5.3 changed to Spring 6.0 in section 13" 's/to Spring 5.3 \(5.3.39\)/to Spring 6.0 (6.0.0)/'
run AC4 "H2 2.x changed to H2 1.4 in section 13"        's/to H2 2.x \(2.2.224\)/to H2 1.4.200/'
run AC4 "link to section 9 broken"                      's/\(#9-configuration\)/(#9-config)/'
run Q6a "technology table back to Spring MVC 4.3"       's/Spring MVC 5.3/Spring MVC 4.3/'
run Q6b "backend test count changed to 68"              's/`mvn clean test` \| 69 \|/`mvn clean test` | 68 |/'
run AC5 "commit touching pom.xml"                       'COMMIT:pom.xml'
run AC5 "commit touching ci.yml"                        'COMMIT:.github/workflows/ci.yml'
run AC5 "commit touching a test source"                 "COMMIT:$(cd "$src" && git ls-files '*Test.java' | head -1)"
rm -rf "$work"
cat "$out"
