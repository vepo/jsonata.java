# Agent Guide — JSONata.java

This document orients AI agents working on this repository. Follow the workflows below and the rules in `.cursor/rules/`.

## Project Overview

| Item | Value |
|------|-------|
| Language | Java 21 (module system) |
| Build | Maven (`mvn test`, `mvn verify`) |
| Tests | JUnit 5, AssertJ, `@Nested` test classes |
| Parser | ANTLR 4 (`MappingExpressions.g4`) |
| JSON | Jackson |
| Quality | SonarCloud |

### Package Layout (DDD-aligned)

```
dev.vepo.jsonata/
├── JSONata.java, JSONataResult.java     # Application / facade
├── parser/                              # Infrastructure (ANT = parse pipeline)
├── functions/                           # Domain (expression evaluation)
│   ├── data/                            # Domain model — JSON value types
│   ├── builtin/                         # Domain services — built-in functions
│   ├── json/, regex/                    # Infrastructure adapters
├── results/                             # Domain — evaluation outcomes
└── exception/                           # Domain errors
```

**Ubiquitous language:** expression, mapping, evaluate, context, built-in function, result node, empty result, group, wildcard, block.

## Workflows

### 1. Domain-Driven Design (DDD)

Use when adding features, refactoring boundaries, or naming new types.

1. **Start with the domain** — What JSONata behavior changes? Name types after spec terms, not Java/ANTLR internals.
2. **Respect layer boundaries** — Domain (`functions/`, `results/`, `exception/`) must not depend on parser or Jackson details. Infrastructure adapts inward.
3. **Extend via domain abstractions** — New built-ins implement existing contracts (`Mapping`, `BuiltInSupplier`). New value shapes extend `Data` or `JSONataResult`.
4. **Keep the facade thin** — `JSONata` orchestrates; it does not embed evaluation logic.
5. **Parser is infrastructure** — Grammar/listener changes produce domain objects (`Mapping`, `Data`), not the reverse.

See `.cursor/rules/ddd-workflow.mdc`.

### 2. Test-Driven Development (TDD)

Use for every behavior change unless the user explicitly skips tests.

1. **Red** — Add a failing test in `src/test/java` mirroring package structure. Use JSONata spec examples or `TestData` fixtures.
2. **Green** — Minimal production code to pass.
3. **Refactor** — Clean up while tests stay green; run `mvn test`.
4. **Structure** — `@Nested` inner classes per feature area; descriptive `void methodName()` test names; AssertJ assertions.
5. **Coverage** — New branches and edge cases (empty, null path, invalid JSON, parse errors) need tests.

See `.cursor/rules/tdd-workflow.mdc`.

### 3. Java Quality (Effective Java)

Apply on every Java change. Rules distill *Effective Java* into actionable checks — see `.cursor/rules/java-effective-java.mdc`.

### 4. Object-Oriented Design (OOP)

Apply when designing types, hierarchies, and APIs. See `.cursor/rules/oop-principles.mdc`.

### 5. Documentation

Update canonical docs in the **same change** when behavior, domain vocabulary, or structure changes. See `.cursor/rules/documentation-workflow.mdc`.

| Document | Update when |
|----------|-------------|
| `DOMAIN_LANGUAGE.md` | New/changed domain terms |
| `ARCHITECTURE.md` | Layers, eval flow, public API, extension points |
| `AGENTS.md` | New workflows or Cursor rules |

## Commands

```bash
git submodule update --init --recursive   # jsonata-js conformance suite
mvn test                                  # unit tests
mvn verify                                # full build + coverage (JaCoCo)
mvn test -Dtest=JsonataConformanceTest#printBaselineReport  # conformance pass rate
mvn -q test-compile exec:java -Dexec.mainClass=dev.vepo.jsonata.conformance.ConformanceDiagnostics -Dexec.classpathScope=test
```

## Agent Conventions

- Match existing style: `var` where types are obvious, minimal comments, Javadoc on public API.
- Do not edit generated ANTLR output under `target/`; change `.g4` and rebuild.
- Prefer small, focused diffs. One concern per commit when the user asks to commit.
- Run tests after substantive changes.
- Update canonical documentation when the change requires it (see Documentation workflow above).

## Cursor Rules Index

| Rule | Scope | Purpose |
|------|-------|---------|
| `architecture.mdc` | Always | Structure, conformance harness, embedding API |
| `documentation-workflow.mdc` | Always | When and how to update canonical docs |
| `ddd-workflow.mdc` | Always | Layer boundaries, domain language document |
| `tdd-workflow.mdc` | Always | Feature-focused TDD, Gherkin scenarios |
| `java-effective-java.mdc` | `**/*.java` | Java idioms and quality checklist |
| `oop-principles.mdc` | `**/*.java` | Encapsulation, composition, Tell Don't Ask |
