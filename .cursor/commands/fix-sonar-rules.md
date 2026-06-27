---
name: Fix Sonar Rules
description: Run local SonarCloud analysis, list every open issue, and fix each one with conservative behavior-preserving changes.
---

You are an expert Java developer working on **JSONata.java**. Your task is to fix **all open SonarCloud issues** on this project. Work **slowly and conservatively** — a clean Sonar report must never come at the cost of broken JSONata behavior, weaker security, or masked bugs.

Align with project rules: [`tdd-workflow.mdc`](../rules/tdd-workflow.mdc), [`java-effective-java.mdc`](../rules/java-effective-java.mdc), [`ddd-workflow.mdc`](../rules/ddd-workflow.mdc), [`in-code-documentation.mdc`](../rules/in-code-documentation.mdc).

Follow this exact loop — **do not ask for confirmation** before editing, but **do stop and report** if a fix would change observable JSONata behavior and the correct behavior is unclear.

## 1. Run local Sonar analysis

Prerequisites: `SONAR_TOKEN` must be set (SonarCloud user token with **Execute Analysis** on `vepo_jsonata.java`). If missing, stop and tell the user to export it:

```bash
export SONAR_TOKEN=<your-sonarcloud-token>
```

Run the same pipeline as CI ([`.github/workflows/build.yml`](../../.github/workflows/build.yml)):

```bash
mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=vepo_jsonata.java
```

If analysis fails on auth, stop — do not skip Sonar and guess issues.

Also run IDE diagnostics:

```bash
mvn -B compile -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true
```

Use `ReadLints` on `src/main/java` and `src/test/java`.

## 2. Fetch all open issues

Pull the full issue list from SonarCloud (paginate until `total` is exhausted):

```bash
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=vepo_jsonata.java&resolved=false&ps=500&p=1"
```

For each issue record: `rule`, `severity`, `component`, `line`, `message`, `type`.

Group by severity (**BLOCKER** → **CRITICAL** → **MAJOR** → **MINOR** → **INFO**), then by file.

Print a numbered work queue before fixing anything.

## 3. Prioritize

Process findings in this order:

1. **Hard failures:** compile errors, failing tests from `verify`.
2. **Security / reliability:** vulnerabilities, bugs, empty catches, swallowed exceptions.
3. **Maintainability on production code:** cognitive complexity, duplicated literals (`java:S1192`), unused code, raw types.
4. **Test smells:** weakened assertions, `@Disabled` without justification.
5. **Workflow / config issues** (e.g. `githubactions:S7637` — pin actions to full commit SHA like `build.yml` does).
6. **Scope:** single-file, localized fixes before cross-cutting refactors.

Skip **won't-fix / false-positive** only after explicit justification in the log (step 6). Never bulk-`@SuppressWarnings` or `//NOSONAR`.

## 4. Before touching code

For each issue (or tight cluster in one file):

1. Read the **file and surrounding class** — match patterns in sibling code.
2. If the issue touches evaluation, built-ins, or parsing → skim [`ARCHITECTURE.md`](../../ARCHITECTURE.md) and [`DOMAIN_LANGUAGE.md`](../../DOMAIN_LANGUAGE.md).
3. Understand **why** Sonar flags it; plan the smallest fix that addresses the root cause.
4. If the issue is in generated ANTLR output under `target/`, fix the **`.g4` grammar or listener** — never edit generated files.

## 5. Fix strategies (prefer refactor over suppression)

| Rule / theme | Safe approach in this repo | Avoid |
|---|---|---|
| `java:S1192` duplicated literals | `private static final` constants | Class-level suppression |
| Cognitive complexity | Extract **private** methods with domain names; keep `Mapping` / `Data` contracts unchanged | Moving domain logic into parser infrastructure |
| Unused imports / dead code | Remove after confirming zero references (built-in registry, tests, parser) | Deleting types registered in `BuiltInSupplier` or grammar |
| Exception handling | Wrap in `JSONataException` with cause; never empty catch | Swallowing parse/eval errors |
| Raw types / unchecked | Narrowest `@SuppressWarnings` with comment on single statement | Class-level suppression |
| Regex DoS (`java:S5852`) | Possessive quantifiers + comment on malformed input | Weaker regex |
| Test smells | Fix assertion or test data; use JSONata spec examples | `@Disabled`, weakened assertions |
| `githubactions:S7637` | Pin `uses:` to full commit SHA (see `build.yml`) | Removing the action |
| Coverage-related Sonar hints | Defer to [increase-code-coverage](increase-code-coverage.md) unless the fix is trivial dead-code removal | Testing generated parser output |

**Never:**

- Add `//NOSONAR` or broad `@SuppressWarnings` without fixing root cause.
- Change public `JSONata` facade behavior to appease Sonar.
- Weaken null checks, validation, or layer boundaries (domain must not import ANTLR/Jackson).
- “Fix” by deleting tests or excluding files in `pom.xml`.

## 6. Verify each batch

After every issue (or same-rule cluster in one file):

```bash
mvn -B test -Dtest=<RelevantTest>   # when behavior might change
mvn -B verify
```

If a test fails, fix the **root cause** (production bug or outdated test). Do **not** proceed until tests are green.

After a large batch, re-run Sonar analysis (step 1) and re-fetch issues (step 2) to confirm the count drops.

## 7. Log every change

Append to `reports/sonar-fix-log-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`:

- Sonar issue key / rule / severity / file:line / message
- Root cause (one sentence)
- Fix strategy
- Files touched
- `mvn verify` result
- If suppressed or marked false positive: explicit justification

Create `reports/` if it does not exist.

## 8. Stop condition

When **all** of the following are clean:

- Sonar issue search returns **0** open issues for `vepo_jsonata.java` (or only justified false positives documented in the log)
- `mvn -B verify` passes
- `ReadLints` on `src/main/java` and `src/test/java` — no unresolved errors

print:

`✅ Sonar rules clean!`

and summarize: issues fixed, any deferred items, final `verify` status, remaining issue count.

## 9. If stuck

Stop the loop and report:

- Sonar rule + message + file
- Why a safe fix is unclear (JSONata spec behavior, architecture, missing test coverage)
- Two concrete options for the user to choose

Do **not** apply a risky workaround.

Start the loop now.
