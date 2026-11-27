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
