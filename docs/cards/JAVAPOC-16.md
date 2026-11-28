# JAVAPOC-16: Add a Java 17 section to the README

## Assess, the card lead, 2026-11-28

### Branch, application, commands
- Branch: `card/JAVAPOC-16`, from `origin/main` at 596d829 (JAVAPOC-15), pushed to origin and linked from the card.
- Worked in a git worktree in the card's scratch folder (`.prime/tmp/JAVAPOC-16/wt` in the programme folder), because the main working copy is on `card/JAVAPOC-8` with an uncommitted change to `docker/local/start-hbase.sh` that is not this card's. That change was left untouched.
- Application: repository root, Maven reactor `sales-batch-parent` with modules `common`, `spark-job`, `workflow-app` (the `ui/` Angular app has its own build and is not touched).
- The project's own test command: `mvn -B test -pl common,spark-job,workflow-app` from the root (the required `test` check, JDK 8). The Java 17 check: `mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8` on JDK 11 and 17 (`java-compat`).

### Tests passing before the change
Source: the project's pipeline on main at 596d829, run https://github.com/aniskhan-trantor/javapoc1.8/actions/runs/46013108111 (conclusion success).

| Job | common | spark-job | workflow-app | Result |
|---|---|---|---|---|
| `test` (JDK 8, required) | 30 pass | 5 pass, 2 skipped | 32 pass | success, 67 pass, 2 skipped |
| `java-compat` (JDK 11) | 30 pass | not run | 32 pass | success, 62 pass |
| `java-compat` (JDK 17) | 30 pass | not run | 32 pass | success, 62 pass |
| `spark-java-compat` (JDK 11 and 17, non-blocking) | 30 pass | 2 errors, 2 skipped | not run | failure, as expected ("Unsupported class file major version") |

### Change map
- Lands in: `README.md` only. A new section (proposed "Java 17", between section 7 "Build and test" and section 8 "API reference", or as a subsection of 7), plus its line in the Contents list. Renumbering the later sections is a choice for Plan; a subsection 7.1 avoids renumbering and broken anchors.
- Sources for the text: `.prime/readiness/java17-upgrade-chain.md` (programme folder), `.github/workflows/ci.yml` (the `java-compat` and `spark-java-compat` jobs), `AGENTS.md` (Build and test), README section 9 (the H2 2.x note on `activiti.jdbc.url`).
- Entry points: none (no code). No pom, workflow or code change (card description).
- Test commands: the pipeline above must stay green and unchanged in counts. A docs card has no behaviour to compare side by side; the acceptance tests in Test design will be checks over `README.md` (for example a small script or test that the section and its commands are present), decided in Plan.

### Slicing
One slice: one file, docs only, and a person can try it by reading the section and running its JDK 17 command. Well within one context budget. No split needed.

### Change readiness
Proceed. No source line changes, nothing on the hold list (`docker/`, runtime versions, `sample-data/`) is touched. Side-by-side run: not applicable (no behaviour changes); the proof is the unchanged pipeline counts above.

### Observations for Plan (not in scope unless the person says so)
- README's technology table (line 12) still says "Spring MVC 4.3"; Spring is 5.3.39 since JAVAPOC-12.
- README section 7 says 67 tests for the backend; the pipeline runs 69 (67 pass, 2 skipped). AGENTS.md says "30 + 7 (2 skipped) + 29"; workflow-app now has 32 (JAVAPOC-15 added tests). AGENTS.md is outside this card's one file.
- The card description's item 4 says the H2 2.x note is "already in section 9"; it is, on the `activiti.jdbc.url` row, so the new section can link to it rather than repeat it.

### Exit criteria
- Branch exists and is linked from the card: yes, `card/JAVAPOC-16`.
- Application folder and its own test command on the card: yes, above (repository root; `mvn -B test -pl common,spark-job,workflow-app`).
- Tests passing before the change: yes, 67 pass and 2 skipped on JDK 8; 62 on JDK 11 and 17 (pipeline run linked).
- One slice a person can try: yes.

### Next step
Plan: the person writes numbered criteria (the lead may offer a draft with defaults), then design and roadmap committed here.

## Plan, the card lead, 2026-11-28

### Criteria (the person's)
Question JAVAPOC-Q6 was answered by the person on 2026-11-28 (defaults accepted). Copied word for word onto the card description:
- AC1 a Java 17 section, listed in Contents, says common and workflow-app run on JDK 11 and 17 (compiled with --release 8), checked by the java-compat job.
- AC2 it gives the local command mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8.
- AC3 it says spark-job cannot run on JDK 11 or 17 yet (Spark 2.4, Scala 2.11) and the cluster (EMR 5.36) runs Java 8.
- AC4 it names Spring 5.3 and H2 2.x, linking section 9.
- AC5 only README.md changes; existing tests unchanged, pipeline passes.
- Item 2 of the same answer: fix the two stale README facts (Spring MVC 4.3, 67 tests) in this card. Not a numbered criterion; checked by two extra checks (Q6a, Q6b below).

Standing answers used: SA-1 (this card file and `docs/cards/JAVAPOC-16-*` do not count against AC5's "only README.md"), SA-2 (existing tests unchanged), SA-3 (tool and layout choices below are the lead's).

### Design
All edits in `README.md`:
1. New last section `## 13. Java 17` (anchor `#13-java-17`), so sections 1 to 12 keep their numbers and anchors. Contents gets `13. [Java 17](#13-java-17)`. Section 7 gets one line: "To run the tests on JDK 17, see [13. Java 17](#13-java-17)."
2. Section 13 content (facts checked against the sources):
   - A short table: `common` and `workflow-app` run on JDK 11 and 17, compiled with `--release 8`, checked by the `java-compat` pipeline job (JDK 11 and 17); `spark-job` not yet, shown by the non-blocking `spark-java-compat` job, which is expected to fail. Source: `.github/workflows/ci.yml` lines 32-72.
   - The local command in a code block: `mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8`, run with `JAVA_HOME` on a JDK 17.
   - Why not `spark-job`: Spark 2.4 (2.4.8) and Scala 2.11 read class files with an old ASM and fail with "Unsupported class file major version" (55 on JDK 11, 61 on JDK 17). The target cluster, EMR 5.36, runs Java 8, so the job keeps running on Java 8 there; moving it needs a newer Spark and an EMR release with Java 17, a decision outside this repository. Source: readiness doc lines 13, 43.
   - What changed for Java 17 readiness: Spring 4.3 to 5.3 (5.3.39, `pom.xml` line 28; it fixed `workflow-app`'s errors on JDK 17), and H2 1.4 to 2.x (2.2.224, `pom.xml` line 32), with a link to [9. Configuration](#9-configuration) for the `activiti.jdbc.url` note on old database files (not repeated).
3. Stale facts (person's item 2): line 12 `Spring MVC 4.3` becomes `Spring MVC 5.3`; section 7's backend row `67` becomes `69` (67 pass and 2 skip without HBase, per the bullet under the table; pipeline run 46013108111).

Not changed (outside AC5's one file): `AGENTS.md` ("30 + 7 (2 skipped) + 29") and `latest-ARCHITECTURE.md` may hold the same stale facts. Left for another card; noted here only.

### Acceptance tests (for Test design)
A bash script `docs/cards/JAVAPOC-16-acceptance.sh` (a card document, SA-1, so AC5 holds; bash and grep run in Git Bash and on the Linux runner). One check function per criterion, named from its number; each prints `PASS` or `FAIL <name>: <what is missing>`, and the script exits 1 if any fails. Section text = from `## 13. Java 17` to the next `## ` or the end of the file.
- `AC1_section_listed_and_scope`: the heading exists; Contents has `13. [Java 17](#13-java-17)`; the section has `common`, `workflow-app`, `JDK 11`, `17`, `--release 8`, `java-compat`.
- `AC2_local_command`: the section has the exact string `mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8`.
- `AC3_spark_job_and_cluster`: the section has `spark-job`, `Spark 2.4`, `Scala 2.11`, `EMR 5.36`, `Java 8`.
- `AC4_changes_and_link`: the section has `Spring 5.3`, `H2 2.`, `](#9-configuration)`.
- `AC5_only_readme`: `git diff --name-only $(git merge-base origin/main HEAD) HEAD` lists only `README.md` and `docs/cards/JAVAPOC-16*`. The rest of AC5 (existing tests unchanged and passing) is the pipeline: same counts as Assess (67 pass and 2 skipped on JDK 8, 62 on JDK 11 and 17).
- `Q6a_spring_version`: README has `Spring MVC 5.3` on the `| API and workflow |` row and no `Spring MVC 4.3`.
- `Q6b_backend_test_count`: section 7's backend row ends `| 69 |`, and no row there says `| 67 |`.

Pipeline: AC5 forbids a workflow change, so the pipeline cannot run the script; the card is exempt from "the pipeline shows the new tests failing". Proof of red instead: the script's output on the test commit (README untouched), recorded here with the commit hash; AC5 is expected to pass on that commit already. The pipeline proves the existing tests stay unchanged and green.

### Roadmap
Test design:
1. Write the script as above; run it on the branch with README untouched; expect AC1 to AC4, Q6a, Q6b to fail and AC5 to pass. Record the output.
2. Sensitivity: confirm each check fails for the right reason (its FAIL line names the missing text), not for a script error.
3. Commit the script and this file; push; open a draft pull request; link it from the card. Record the pipeline exemption.
Build:
4. Add section 13, the Contents line and the pointer in section 7.
5. Fix line 12 and the section 7 count.
6. Run the script: all PASS. Push; check the pipeline on that commit passes with the same counts; check the diff is README.md plus card documents only; mark the pull request ready for review.

### Decisions taken without asking (SA-3)
- Section at the end as 13, not a renumbered 8: no anchors break.
- Acceptance tests as a script in `docs/cards/`, not a JUnit test: a test in a module would break AC5.

### Exit criteria
- Numbered criteria written by a person are on the card: yes, JAVAPOC-Q6 answer, copied to the card description.
- Design and roadmap committed on the card's branch: yes, this section (commit in the done comment).
- A fresh-context helper read the design, every finding resolved or answered: gap, see "Design review" below.

### Design review
Gap: no helper with fresh context could be started from this session. It has no subagent tool, and running the `claude` command line from the shell needs a person's approval, which a non-interactive run cannot get. Recorded as a gap outside the card (charter: write it down and go on). In its place: (a) the lead checked the design against its sources, findings below; (b) Review on this card is blind and comes with fresh context, so an independent reader still sees the result before Verify.

Lead's check against the sources (each resolved in the design above):
1. Facts: `--release 8` and the two commands match `.github/workflows/ci.yml` lines 30, 50, 72; Spring 5.3.39 and H2 2.2.224 match `pom.xml` lines 28 and 32; "Unsupported class file major version 55 / 61" and EMR 5.36 on Java 8 match the readiness doc lines 13 and 43. Resolved: no change.
2. The anchor `#13-java-17` is GitHub's slug for `## 13. Java 17` (the same pattern as `#7-build-and-test`). Resolved: no change.
3. 69 is right for `mvn clean test`: Surefire counts 69 tests run; the 2 HBase tests skip when HBase is down (the bullet under the table already says so). Resolved: write 69.
4. `AC5_only_readme` needs `origin/main` to be current: the script runs `git fetch origin main` first and fails with a clear message if it cannot. Added to Test design step 1.
5. `AC1` checking a bare `17` is weak (any number with 17 passes). Resolved: check for `JDK 11` and `JDK 17`, or the phrase `11 and 17`.
6. The technology table says "Java 8" for the API; that stays true (it builds for Java 8). Section 13 opens by saying the project builds and ships on Java 8, so the two do not contradict. Resolved: only the Spring version changes on line 12.

## Test design, the card lead, 2026-11-28

### Tests
`docs/cards/JAVAPOC-16-acceptance.sh` (run: `bash docs/cards/JAVAPOC-16-acceptance.sh`; exit 0 all pass, 1 any fail, 2 cannot run). One check per criterion, name starting with its number, as in the Plan design, plus the two checks for the person's item 2:

| Criterion | Test |
|---|---|
| AC1 | `AC1_section_listed_and_scope` |
| AC2 | `AC2_local_command` |
| AC3 | `AC3_spark_job_and_cluster` |
| AC4 | `AC4_changes_and_link` |
| AC5 | `AC5_only_readme` (the diff), plus the pipeline: existing tests unchanged and passing |
| Q6 item 2 | `Q6a_spring_version`, `Q6b_backend_test_count` |

No criterion without a test; no test without a criterion or the person's answer.

### Red run
On the test commit (the commit that adds this section and the script; `README.md` identical to `main` at 596d829), Git Bash on Windows:

```
FAIL AC1_section_listed_and_scope: no '## 13. Java 17' heading
FAIL AC2_local_command: section 13 lacks 'mvn -B test -pl common,workflow-app -Dmaven.compiler.release=8'
FAIL AC3_spark_job_and_cluster: section 13 lacks '`spark-job`' 'Spark 2.4' 'Scala 2.11' 'EMR 5.36' 'Java 8'
FAIL AC4_changes_and_link: section 13 lacks 'Spring 5.3' 'H2 2.' '](#9-configuration)'
PASS AC5_only_readme
FAIL Q6a_spring_version: README still says 'Spring MVC 4.3'
FAIL Q6b_backend_test_count: backend row does not end '| 69 |': | Java and Scala (`common`, `spark-job`, `workflow-app`) | `mvn clean test` | 67 |
failures: 6
exit=1
```

Each FAIL names the missing text, so it fails for the right reason, not a script error. `AC5_only_readme` passes here by design (only card documents changed so far); its sensitivity check (a scratch commit that touches another file must make it fail) is done in Build, in a throwaway worktree in the scratch folder.

### Pipeline exemption
AC5 forbids a workflow change, so the pipeline cannot run this script and cannot show the new tests failing. The red run above stands in. The pipeline's job on this card is the other half of AC5: the existing tests unchanged and passing with the Assess counts (67 pass and 2 skipped on JDK 8, 62 on JDK 11 and 17). The verifier reruns the script itself on the latest commit.

Pipeline on the test commit 98279e3: run https://github.com/aniskhan-trantor/javapoc1.8/actions/runs/46116279916, success (existing tests only, as above).

## Build, the card lead, 2026-11-28

### What changed
Commit acbf66a, `README.md` only: section 13 "Java 17" at the end (table of modules, the local JDK 17 command, why `spark-job` cannot yet, what changed with a link to section 9), its Contents line, a pointer from section 7, `Spring MVC 4.3` to `5.3` on the technology table, and the backend test count 67 to 69. The acceptance script is unchanged since the test commit 98279e3.

### Green run
`bash docs/cards/JAVAPOC-16-acceptance.sh` on acbf66a, Git Bash on Windows: all seven PASS, `failures: 0`, exit 0.

### Sensitivity check
In a throwaway worktree at acbf66a (scratch folder, removed afterwards; script `.prime/tmp/JAVAPOC-16/sensitivity.sh` in the programme folder), one fact broken at a time, README restored between:

| Broken | Result |
|---|---|
| Contents line removed | FAIL AC1 (Contents lacks the line) |
| `--release 8` removed | FAIL AC1 |
| local command removed | FAIL AC2 |
| "Java 8" removed from section 13 | FAIL AC3 |
| "Scala 2.11" removed | FAIL AC3 |
| link to section 9 removed | FAIL AC4 |
| `Spring MVC 4.3` restored | FAIL Q6a |
| `67` restored | FAIL Q6b |
| a commit touching `pom.xml` | FAIL AC5 (names `pom.xml`) |

Each break failed only its own check (`failures: 1` every time).

### Note for the verifier
The script is stored with LF endings. On Windows with `core.autocrlf=true` it is checked out with CRLF; Git Bash ran it here without trouble. If a shell complains about `\r`, strip them in the local checkout first (`sed -i 's/\r$//' docs/cards/JAVAPOC-16-acceptance.sh`; git sees no change, as the stored file is LF).

### Exit criteria
- Pipeline on the latest commit passes, the acceptance tests unchanged: see the done comment for the run; the script is unchanged since 98279e3.
- Pull request ready for review: https://github.com/aniskhan-trantor/javapoc1.8/pull/11.

## Verify, Verify, 2026-11-28

Verdict: every criterion holds. One verifier only (see Gaps). Briefed from the tracker and this file; the README diff was read because the README is this card's artifact (no source code changed).

### Where it ran
- Fresh clones of `main` (596d829) and the PR head (258d50d) in the programme's scratch folder `.prime/tmp/JAVAPOC-16/`. The PR head's merge base is 596d829, so main is the right baseline.
- Pipeline on the PR head 258d50d: run https://github.com/aniskhan-trantor/javapoc1.8/actions/runs/46116481882, `test`, `java-compat (JDK 11)` and `java-compat (JDK 17)` success; the two `spark-java-compat` jobs fail as they do on main (non-blocking, "Unsupported class file major version 55" and "61" in the log). Baseline: main run https://github.com/aniskhan-trantor/javapoc1.8/actions/runs/46013108111.
- The acceptance script cannot run in the pipeline: AC5 (the person's) rules out a workflow change. Stand-in for the pipeline: the script run from the fresh PR-head clone in Git Bash. A clean Linux container was not available to this run (Docker needs a person's approval here).

### Per criterion
| Criterion | Test | Run | Result |
|---|---|---|---|
| AC1 | `AC1_section_listed_and_scope` | stand-in, fresh clone at 258d50d | PASS |
| AC2 | `AC2_local_command` | stand-in, fresh clone at 258d50d | PASS |
| AC3 | `AC3_spark_job_and_cluster` | stand-in, fresh clone at 258d50d | PASS |
| AC4 | `AC4_changes_and_link` | stand-in, fresh clone at 258d50d | PASS |
| AC5 | `AC5_only_readme`, plus the pipeline | stand-in; pipeline run 46116481882 | PASS; pipeline success, test counts identical to main (below) |
| Q6 item 2 | `Q6a_spring_version`, `Q6b_backend_test_count` | stand-in, fresh clone at 258d50d | PASS |

Red run: the same script on the fresh `main` clone fails six of seven (AC1 to AC4, Q6a, Q6b; AC5 passes by design), the same lines as the builder's red run on 98279e3 in "Test design" above. Pipeline on the test commit: https://github.com/aniskhan-trantor/javapoc1.8/actions/runs/46116279916.

Facts checked by reading against their sources, not only by the script: Spring 5.3.39, H2 2.2.224, Spark 2.4.8 and Scala 2.11 match `pom.xml`; the command matches `.github/workflows/ci.yml` line 50 and the job names exist; "Unsupported class file major version 55 / 61" is in the pipeline log; EMR 5.36 matches the readiness doc and README's own technology table; `## 9. Configuration` exists and its `activiti.jdbc.url` row carries the H2 2.x note; 69 = 30 + 32 + 7 tests in the `test` job.

### Side-by-side
- Code: `git diff origin/main HEAD` touches `README.md` and `docs/cards/JAVAPOC-16*` only. No source, build, test or workflow file differs, so the built artifacts and public API are the same by construction; the input sweep and environment axis have nothing to vary.
- Pipeline: per-job test totals (Surefire summaries) of the main run and the PR-head run are identical, every job (`test` 30 / 32 / 7 with 2 skipped; `java-compat` 30 / 32 on JDK 11 and 17; `spark-java-compat` 30 and 7 with 2 errors, 2 skipped, both JDKs).
- Known-differences ledger: none on the card, and none needed; no difference found.

### Sensitivity check
Verify's own 19 single-fault mutants of README (and three commits touching other files), not the builder's recipe: `docs/cards/JAVAPOC-16-verify-mutants.sh`, results `docs/cards/JAVAPOC-16-verify-mutants.txt`. 15 KILLED by the named check; every check is killed by at least one mutant. 4 SURVIVED, a finding about the checks, not the README:
- "JDK 11 and 17" cut to "JDK 11": AC1 still passes, because "JDK 17" appears in the local-command paragraph.
- "the `java-compat` pipeline job" removed: AC1 passes, because `spark-java-compat` contains `java-compat`.
- the `workflow-app` table row removed: AC1 passes, because `` `workflow-app` `` appears in the "What changed" list.
- the cluster's "Java 8" changed to "Java 11": AC3 passes, because "Java 8" appears in the section's first line.
The checks test that words are present in section 13, not that they sit in the right sentence. They are a docs card's checks and are not run by the pipeline, so they guard only this card; the README itself was read and meets each criterion. Not a send-back; noted for the person in the accept question.

### Gaps
- Second verifier (the workflow's `verifiers: 2`, on Sonnet): not started. This run has no subagent tool and the `claude` command line needs a person's approval. One verdict, not two.
- "Every criterion's test passed in the pipeline": holds for AC5's pipeline half; AC1 to AC4 and Q6 run on the stand-in only, because the person's AC5 forbids the workflow change that would put them in the pipeline.
