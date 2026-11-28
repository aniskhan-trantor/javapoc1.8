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
