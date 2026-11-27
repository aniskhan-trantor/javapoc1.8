# JAVAPOC-2: Add a pipeline that runs the tests

## Assess, Ana, 2026-11-27

### Branch, application, commands
- Branch: `card/JAVAPOC-2`, from main (78c482b), pushed to origin.
- Application: repository root, Maven reactor `sales-batch-parent` with modules `common` (Java), `spark-job` (Scala 2.11, Spark 2.4.8), `workflow-app` (Java 8, Spring MVC, Activiti). The `ui/` Angular app has its own build and is out of this card.
- Test command: `mvn -B test` from the root. Tools here: Java Temurin 1.8.0_512, Maven 3.9.9.

### Baseline (clean clone of main, `mvn -B test -fae`)
| Module | Result |
|---|---|
| common | 30 tests run, 0 failed, 0 skipped (about 14 s) |
| spark-job | 7 run, 0 failed, 2 skipped (HBase-dependent test uses `assumeTrue`, skipped when HBase is not on localhost) (about 49 s) |
| workflow-app | **test compilation fails**, 0 tests run |

Passing before the change: 35 (30 + 5), 2 skipped, workflow-app not run. Reactor result: BUILD FAILURE.

Cause: the test sources in `workflow-app/src/test` reference three classes that are not in the repository and never were in its history: `TestInfraConfig`, `LocalJvmSparkJobLauncher`, `DockerClusterSparkJobLauncher` (the last two in package `workflow.spark`). `docs/project-summary.md` calls them "IDE only (test sources)". A working copy that had stale `target/` output from an earlier build appeared to compile; a clean checkout does not. This is the "do not install in a clean container" fact behind the hold list (facts.md).

So a pipeline that runs `mvn test` on the whole reactor would be red on its first run, and every pull request after it, until those files are restored. Protecting main with a required red check would block all merges.

### Change readiness (`--intent`, judged by hand)
The readiness scripts were not run: the change touches no source line (one new file under `.github/workflows/`), so there is nothing for a canary or covering tests to measure. Context used: `readiness.json` (2026-11-27, not ready for agent-led change, score 1.4) and the clean-clone baseline above.

**Verdict: proceed with review, for the pipeline on the two modules that build; mitigate first for the whole reactor** (a gate that cannot pass would notice every break and also block every merge). It does not touch the hold list (workflow-app, spark-job code) as long as the pipeline is only a new workflow file.

### Side-by-side
Not applicable as a behaviour comparison: no application code changes, so there is no old and new behaviour. The equivalent proof is the clean-clone run above (input: 3 modules; difference between "works on the working copy" and "works on a clean checkout": 1 module, workflow-app, compile errors in 5 test files from 3 missing classes). Log: `C:/Users/anees.khan/.readiness/javapoc1.8/jp2-baseline.log` (outside the repository). Scratch clone was in the system temp folder.

### INVEST (the pipeline, as scoped below)
- Independent: yes. One new workflow file; needs no other card.
- Negotiable: yes. Hosted runner, Java 8, `mvn -B test` with a Maven cache; the exact modules are the decision below.
- Valuable: yes. Every pull request gets a recorded pass or fail; the goal's "building, tested" has no proof today.
- Estimable: yes. About 30 lines, well under a day.
- Small: yes. About 30 lines, one pull request, a roadmap of 2 steps.
- Testable: yes. The first run on the pull request shows the counts above (30, and 5 passed with 2 skipped).

The card as written ("modules common, spark-job, workflow-app") is not one slice only because it hides a second piece of work: restoring the missing workflow-app test sources (or deciding they are IDE-only and moving them out). That piece could not go first and cannot be in this card, since the hold list bars changing workflow-app until it installs and has tests that run, and the missing files' content is not in the repository to recover. Name of the first part that can merge alone: the pipeline on `common` and `spark-job`.

### Proposed split (default)
1. JAVAPOC-2, as it stands but with scope `-pl common,spark-job` (adds `.github/workflows/ci.yml`; runs on pull requests and on pushes to main). Proved by its own first run: 35 passed, 2 skipped.
2. New card: make workflow-app's tests compile and run (restore or rewrite the three missing IDE-only classes; widen the pipeline to the whole reactor). Needs a person to lift the hold list for workflow-app. Goes after slice 1.
3. Protect main (required check = the pipeline). This is a repository setting, not a code change, and needs a person with admin rights on the repository; I do not change it. It follows the first green run of slice 1, naming only the checks that can pass.

### Decisions for Plan (proposed defaults)
1. Scope of the pipeline: default `common` and `spark-job` only now; workflow-app joins in the new card (alternative: whole reactor, red from the start; or fix workflow-app inside this card, which breaks the hold list).
2. Branch protection: default a person sets it after the first green run, requiring the pipeline check; alternative: not now.
3. Accept the split above: JAVAPOC-2 (narrowed), a new card for workflow-app tests, in that order.

No questions for the business meaning. No standing answer needed beyond SA-3.

### nWave
`nw:discuss` was not run. Facts: nWave is an optional pack, not used by default in this programme. For a 30-line workflow file the discuss wave adds nothing (no user journey). Decided by me under SA-3; criteria are Plan's, written by a person.

### Gaps
- Readiness scripts and containers not run (reason above).
- HBase-dependent test is skipped everywhere HBase is absent, including the pipeline.
- Java 8 on the hosted runner: Temurin 8; Scala 2.11 comes from the Maven build.

## Plan, Archie, 2026-11-27

### Slices
Settled by the person's answer to JAVAPOC-Q4 (all three defaults accepted): this card is the pipeline on `common` and `spark-job`; a new card will restore workflow-app's tests and widen the pipeline (hold list lifted for it by a person); branch protection is set by a person with admin rights after the first green run. Not asked again.

### Design
- One new file, `.github/workflows/ci.yml`. No source, build or test file changes, so the hold list and SA-2 are untouched.
- Triggers: `pull_request` and `push` to `main`.
- Job `test` on `ubuntu-latest`: checkout, `actions/setup-java` with Temurin 8 and `cache: maven`, then `mvn -B test -pl common,spark-job`. The job name `test` is the check name a person will require on main.
- No artifacts uploaded; Surefire results show in the run log. Publishing Jacoco reports is a later card.
- Java 8 and Scala 2.11 are unchanged: the pipeline uses the repo's own Maven pins.
- The HBase-dependent tests skip themselves (2 skipped), as on the baseline.
- No codemod or recipe needed; one hand-written file.

### Roadmap (2 steps)
1. Add `.github/workflows/ci.yml`, push on `card/JAVAPOC-2`, open a draft pull request.
2. Read the first run: `common` 30 pass; `spark-job` 5 pass, 2 skipped; run green. If it differs from the clean-clone baseline, fix the file only.

### Decisions I took (SA-3)
- nWave design and roadmap waves not run: for one 30-line file they add nothing; same reasoning as Assess. The fresh-context read of the design is done in Test design.
- `-pl common,spark-job`, not the full reactor: workflow-app's tests do not compile on a clean checkout, so the full reactor is red until the next card.
- Temurin 8 on a hosted runner, because the baseline was measured on Temurin 1.8.0_512.

### Criteria
Written by the person (answer to JAVAPOC-Q5, all four defaults accepted; copied word for word to the card description):
- AC1: A pipeline definition in the repository runs 'mvn -B test -pl common,spark-job' on Java 8 for every pull request and every push to main.
- AC2: On the pull request, that run passes: all common tests and all spark-job tests pass, and the HBase-dependent tests may be skipped.
- AC3: The change adds or edits no file other than the pipeline definition and the card file; the existing tests are unchanged.
- AC4: The pipeline check is named 'test', so a person can require it on main.

### Fresh-context read of the design (Plan exit criterion)
A helper with no prior context read this file, the three poms and the HBase test. No blocking finding: spark-job depends only on `common`, the reactor orders it, nothing needs credentials, Docker or HBase, and the HBase test skips itself. Four minor findings, all resolved in the design:
1. The check shows in the UI as `<workflow name> / test`: the workflow gets `name: CI`; the job id and name stay `test` (AC4 is about the job name; unchanged).
2. A failing test must fail the job: Surefire's default does this; the workflow sets no `-Dmaven.test.failure.ignore`. AC2 is read from the first run (roadmap step 2).
3. A cold Maven cache downloads Spark, Hadoop and HBase: add `timeout-minutes: 20` to the job.
4. Superseded pull request runs: add a `concurrency` group that cancels in-progress runs on the same ref for pull requests.
Criteria untouched. The additions sit in the same single file, so AC3 holds.

## Test design, Tess, 2026-11-27

### Decision: all four criteria are observed, no test file is committed
Every criterion here is about a pipeline definition, a pipeline run, or the list of files in the change. AC1 and AC4 could be checked by a script that reads `.github/workflows/ci.yml`, but any test file I add is a file other than the pipeline definition and the card file, and AC3 forbids that. I may not weaken AC3, and a test that breaks AC3 would fail the card by existing. So no test file is added; each criterion is observed as below (decided under SA-3; nWave `distill` produces executable tests and has nothing to produce here, so it was not run). The pipeline-on-the-test-commit exit criterion is exempt for this reason: the test commit adds nothing for a pipeline to run, and before the build there is no pipeline at all, which is the failing state of every criterion.

### How each criterion is observed (Verify follows this, source-blind, on the pull request's head)
| Criterion | Observation | Passes when |
|---|---|---|
| AC1 | `gh run list --branch card/JAVAPOC-2` and the run's log; the trigger is read from the run's event (`pull_request`), and a run on main after the merge shows `push` | The log shows `mvn -B test -pl common,spark-job` under a Java 1.8 JDK; events `pull_request` and `push` on main both appear |
| AC2 | `gh pr view --json statusCheckRollup` and `gh run view --log` on the head commit | Run concludes success; log shows common 30 run, 0 failed; spark-job 7 run, 0 failed, at most 2 skipped (the HBase test); Reactor SUCCESS |
| AC3 | `gh pr view --json files` (or `git diff --name-only main...head`) | Files are only `.github/workflows/ci.yml` and `docs/cards/JAVAPOC-2.md` (and other files under `docs/` from agents, per SA-1); no path under `common/`, `spark-job/`, `workflow-app/` `src/` or any `pom.xml` |
| AC4 | `gh pr view --json statusCheckRollup` | A check whose name is exactly `test` (shown by GitHub as `CI / test`; the job name is `test`) |

### Red state before the build
Nothing runs on this commit: `main...HEAD` holds only the card file, and no workflow exists. Every criterion is unmet: no run, no check named `test`. That is the "new tests fail" state for new behaviour.

### Sensitivity check for Build (kept behaviour is not the case here, but the pipeline must be shown able to fail)
After the first green run, push one throwaway commit that changes the command in `ci.yml` to `mvn -B test -pl workflow-app` (its tests do not compile on a clean checkout). The job `test` must turn red. Then revert that commit (`git revert`) so the head is green and AC3 holds on the final file list. Keep the red run's link: Verify's evidence lists it.

### Gaps
- No executable test exists for AC1 to AC4; they are read from the pipeline's own record, which is also what Verify has to use.
- AC2's counts come from the baseline in Assess (30, and 5 passed with 2 skipped); the first run is the first time they are measured on a hosted runner.

## Build, Engie, 2026-11-27 (in progress)

- Step 1 done: `.github/workflows/ci.yml` added (workflow `CI`, job `test`, triggers pull_request and push to main, Temurin 8 with Maven cache, `mvn -B test -pl common,spark-job`, 20 minute timeout, concurrency group cancelling superseded PR runs), pushed on `card/JAVAPOC-2` (commit 73880c8).
- Step 2 not done: `gh` calls (pull request, run list, run log) were refused by the permission system in this session, so I could not open the draft pull request, read the first run, run the sensitivity check, or mark the PR ready. No test or source file was touched (AC3).
- Still to do: open or find the pull request, read the run on the latest commit (expect common 30 pass; spark-job 7 run, 2 skipped), run Tess's sensitivity check (throwaway commit switching to `-pl workflow-app`, expect red, then `git revert`), link the red run, mark ready.
