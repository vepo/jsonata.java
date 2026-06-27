---
name: Increase Code Coverage
description: Run tests with JaCoCo, rank classes by uncovered lines, and add feature tests for missing use cases and edge conditions.
---

You are an expert Java test engineer working on **JSONata.java**. Your goal is to **raise JaCoCo coverage** by testing real JSONata behavior — not by weakening assertions or testing implementation details.

Align with [`tdd-workflow.mdc`](../rules/tdd-workflow.mdc), [`ddd-workflow.mdc`](../rules/ddd-workflow.mdc), [`DOMAIN_LANGUAGE.md`](../../DOMAIN_LANGUAGE.md), and the [JSONata spec](https://docs.jsonata.org).

Current SonarCloud baseline (approximate): **~75% line**, **~68% branch**. Improve both; branch gaps are usually the binding constraint.

Follow this exact loop — **do not ask for confirmation** before writing tests.

## 1. Measure current coverage

```bash
mvn -B clean verify
```

JaCoCo report (configured in [`pom.xml`](../../pom.xml)):

- XML: `target/site/jacoco/jacoco.xml`
- HTML: `target/site/jacoco/index.html`

Parse report-level counters for **INSTRUCTION** and **BRANCH**. Print both percentages every iteration.

## 2. Rank classes by uncovered lines

From `jacoco.xml`, build a work queue:

1. For each `<class>` under `src/main/java` (skip generated ANTLR under `functions/generated/`, synthetic `$` types, and `module-info`).
2. Compute **missed instructions** = `missed` on the class-level `INSTRUCTION` counter.
3. Sort descending by missed instructions.
4. Also list the **5 lowest branch-coverage** classes (min 20 instructions, skip generated/synthetic).
5. **Prioritize branch gaps** when choosing the next target.

Print the top 10 classes with: FQCN, instruction %, branch %, missed lines/branches.

## 3. Analyze uncovered code (before writing tests)

For each target class, open the HTML report or read the source and identify **what behavior is untested**:

| Uncovered pattern | Likely use case / edge condition to test |
|---|---|
| `if` / `else` branch | Happy path vs error/empty/missing input |
| `switch` / operator dispatch | Each operator variant from JSONata spec |
| `catch` block | Invalid JSON, parse error, type mismatch |
| Early `return` / guard | Null context, empty array, missing path |
| Default in `switch` | Unknown token or unsupported combination |
| Private helper only called from one path | Test through **public** `JSONata.jsonata(expr).evaluate(...)` API |

For each gap, write one sentence: **"Uncovered because we don't test …"** before adding a test.

Reference [`DOMAIN_LANGUAGE.md`](../../DOMAIN_LANGUAGE.md) for naming scenarios; use JSONata spec examples where possible.

## 4. Add or extend tests

**Structure** (per tdd-workflow):

- Prefer feature tests: `src/test/java/dev/vepo/jsonata/features/<Feature>FeatureTest.java`
- `@Nested` class = **Scenario**; `@Test` = **Example**
- Gherkin comments: `// Given …` / `// When …` / `// Then …`
- AssertJ only (`assertThat`, `assertThatThrownBy`)
- Evaluate through public API: `JSONata.jsonata(expression).evaluate(json)`

**Rules:**

- Every test method must **assert observable results** (return value, text, array contents, thrown `JSONataException` message).
- Never call production code without assertions.
- Do **not** delete or weaken existing assertions.
- Do **not** test generated ANTLR lexer/parser classes directly — test via expressions.
- After adding/changing a test, run it in isolation:

```bash
mvn -B test -Dtest=FeatureNameFeatureTest
```

## 5. Prefer removal over testing dead code

Only delete production code when **confirmed unreachable**:

1. Search repo for class/method references (built-in registry, grammar, tests).
2. Respect layer boundaries — don't remove infrastructure adapters still wired in parser.
3. If unsure, **add a test** instead of deleting.

After removal: `mvn -B verify` and re-measure coverage.

## 6. Re-measure and repeat

After covering one class (or a tight cluster):

```bash
mvn -B clean verify
```

Re-parse `jacoco.xml`. Log **instruction %** and **branch %** before and after.

Append to `reports/coverage-log-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`:

- Target class(es)
- Uncovered lines/branches identified
- Use case / edge condition added
- Test class and scenario names
- Coverage before → after (instruction & branch)
- `mvn verify` result

Create `reports/` if it does not exist.

Repeat from step 2 until:

- **Branch coverage ≥ 80%** and **instruction coverage ≥ 80%**, **or**
- Remaining gaps are documented as generated code, unreachable defensive branches, or require user decision.

When thresholds are met, print:

`✅ Coverage target reached! (instruction and branch ≥ 80%)`

## 7. Stop condition (partial progress)

If you cannot reach 80% in one session, stop with a clear summary:

- Top remaining low-coverage classes
- Specific untested use cases / edge conditions still missing
- Recommended next tests (expression + expected result)

## 8. If stuck

Stop and report:

- Class and uncovered lines
- Why behavior is unclear (spec ambiguity, untestable private API)
- Two options: refactor for testability vs conformance test from jsonata-js suite

Do **not** add `@SuppressWarnings`, exclude packages in JaCoCo, or mock away domain logic.

Start the loop now.
