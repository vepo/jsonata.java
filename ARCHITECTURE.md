# Architecture — JSONata.java

**Canonical description of system structure.** Agents must read this before structural or cross-cutting work and update it when architecture changes.

Related: [`DOMAIN_LANGUAGE.md`](DOMAIN_LANGUAGE.md) (vocabulary) · [`AGENTS.md`](AGENTS.md) (agent workflows)

---

## Purpose

JSONata.java is a **Java 21 library** that parses and evaluates [JSONata](https://docs.jsonata.org) expressions against JSON input. It is a native implementation built around ANTLR for parsing and Jackson for JSON I/O.

**Public API:**

```java
JSONata jsonata = JSONata.jsonata(expr)
    .bind("name", jsonNode)
    .registerFunction("fn", call -> { ... });
jsonata.evaluate(jsonString);
```

Also: `JSONata.jsonata(expr, EvaluationEnvironment)` for bindings at parse time.

---

## High-Level Flow

```mermaid
flowchart LR
    subgraph compile [Compile time]
        A[Expression text] --> B[ANTLR lexer/parser]
        B --> C[MappingExpressionsListener]
        C --> D[List of Mapping]
    end

    subgraph runtime [Runtime]
        E[JSON input text] --> F[Data.load]
        F --> G[Mapping.map]
        G --> H[Data]
        H --> I[Data.toNode]
        I --> J[JSONataResult]
    end

    D --> G
```

1. **Parse** — Expression string → ANTLR parse tree → listener builds `Mapping` objects (compile to evaluators).
2. **Load** — JSON string → `Data` domain model via `JsonFactory`.
3. **Evaluate** — `Mapping.map(original, context)` walks/transforms `Data`; facade converts to `JSONataResult`.

Multi-statement expressions are composed by reducing the mapping list in `JSONata.evaluate`.

---

## Compile-Time vs Evaluation-Time

The library follows **compile-then-evaluate**: parsing builds an executable `Mapping` tree; evaluation runs that tree against input data. Expression text is **not** re-parsed during normal evaluation.

### Parse time (compile)

Triggered only by `JSONata.jsonata(expr)` → `MappingParser.parse()`.

| What happens | What does *not* happen |
|--------------|------------------------|
| ANTLR lex/parse of the JSONata expression | No input JSON is read |
| `MappingExpressionsListener` assembles `Mapping` objects | No `Mapping.map()` against user data |
| Variables, blocks, and lambdas captured in closures | No result values computed |

The listener is a **compiler**, not an interpreter. Each grammar exit handler pushes evaluators onto a stack; the output is a `List<Mapping>` stored in `JSONata`.

### Evaluation time (execute)

Triggered by `evaluate()` / `evaluateData()`.

| What happens | What does *not* happen |
|--------------|------------------------|
| Input JSON loaded via `Data.load()` (Jackson) | No ANTLR / `MappingParser` (except documented exceptions below) |
| `Mapping.map(original, context)` walks the compiled tree | Expression text is not tokenized again |
| `Data.toNode()` produces `JSONataResult` | Parse tree is not retained or re-walked |

Entry point: `JSONata.evaluateData()` — only invokes pre-built mappings.

### Evaluation strategy

**Path and array steps are eager** — when a step is reached, `MappingJoin` maps over the current context (including array expansion). This matches JSONata semantics.

**Conditional branches are lazy** — only the taken path runs:

- `InlineIf` evaluates the test, then either the true or false branch (not both).
- `Coalesce` evaluates the right side only when the left is empty.

This is lazy *branch* evaluation at execute time, not lazy *compilation*.

### Documented violations (runtime work at evaluate time)

These are intentional departures from strict compile-once. Each is justified.

| Violation | Location | Justification |
|-----------|----------|---------------|
| **`$eval` re-parses JSONata** | `Eval.map()` calls `MappingParser.parse(expr)` | JSONata spec requires dynamic evaluation of expression strings only known at runtime. The argument is data-dependent; it cannot be compiled when the outer expression is parsed. |
| **Input JSON parsing** | `Data.load()` in `evaluate()` | Input document is external data, not part of the expression AST. Must be read when evaluation runs (and may change between calls on the same compiled `JSONata`). |
| **Regex engine init** | `RegExp` constructor (Nashorn) on first use of a regex literal | Compiles the regex *pattern* for matching, not JSONata syntax. Pattern text is fixed at expression parse time; engine setup is deferred until the literal is evaluated. |
| **Value parsing** | `ToMillis`, `ParseInteger`, etc. | Parses date/number *values* at execute time, not JSONata expressions. |
| **Deep copy via JSON** | `Transform` uses `JsonFactory.fromString` for copy semantics | Serializes/deserializes `Data` to clone structure during transform; not expression re-parsing. |

**Single ANTLR entry point:** `MappingParser` — called from `JSONata.jsonata()` and from `$eval` only.

### Bindings after compile

`JSONata.bind()` / `registerFunction()` merge into `EvaluationEnvironment` but **do not re-parse** the expression. Bindings that affect variable resolution must be supplied via `JSONata.jsonata(expr, env)` at compile time. Calling `bind()` after `jsonata()` reuses the existing `List<Mapping>` unchanged.

### Agent rule

When adding features:

- **Do** build new behavior as `Mapping` implementations wired in the listener.
- **Do not** call `MappingParser.parse()` from `map()` unless implementing spec-mandated dynamic evaluation (like `$eval`).
- **Do not** evaluate against input JSON inside `MappingExpressionsListener`.

---

## Layered Architecture

| Layer | Package(s) | Responsibility |
|-------|------------|----------------|
| **Application** | `dev.vepo.jsonata` | Facade (`JSONata`), public result type (`JSONataResult`) |
| **Domain** | `functions/`, `results/`, `exception/` | Expression semantics, evaluation, result model |
| **Infrastructure** | `parser/`, `functions/json/`, `functions/regex/` | ANTLR pipeline, Jackson adapters, Nashorn regex |

**Dependency rule:** Infrastructure and application depend on domain abstractions. Domain logic lives in `Mapping` implementations and `Data` subtypes — not in the listener beyond assembly.

```
dev.vepo.jsonata/
├── JSONata.java                 # Application facade
├── JSONataResult.java           # Public result contract
├── parser/                      # Infrastructure — parse pipeline
│   ├── MappingExpressionsListener.java   # Parse tree → Mapping
│   ├── BuiltInFunction.java              # Built-in registry
│   └── JSONataValidator.java
├── functions/                   # Domain — evaluation core
│   ├── Mapping.java             # Central evaluator interface
│   ├── MappingParser.java       # Orchestrates ANTLR (infra entry)
│   ├── MappingJoin.java         # Path composition (.)
│   ├── BlockContext.java        # Variables & functions in blocks
│   ├── data/                    # JSON value model during eval
│   ├── builtin/                 # Built-in function implementations
│   ├── json/                    # Jackson adapter (infra)
│   └── regex/                   # Regex engine adapter (infra)
├── results/                     # Domain — JSONataResult implementations
└── exception/
    └── JSONataException.java
```

Grammar source: `src/main/antlr4/.../MappingExpressions.g4` → generated code under `target/generated-sources/antlr4/`.

---

## Core Abstractions

### Mapping

The heart of evaluation. Every expression construct compiles to a `Mapping`:

```java
Data map(Data original, Data current);
```

| Parameter | JSONata equivalent | Role |
|-----------|-------------------|------|
| `original` | `$$` | Root input document |
| `current` | `$` | Context at this evaluation step |

Mappings compose via `andThen`, `MappingJoin` (paths), and nested structures (blocks, functions).

### Data

In-memory JSON value during evaluation. Subtypes: `ObjectData`, `ArrayData`, `GroupedData` (multi-match sequences), `EmptyData`, `RegexData`.

- **Inbound:** `Data.load(String)` → Jackson parse → domain types
- **Outbound:** `Data.toNode()` → `JSONataResult`

### JSONataResult

Caller-facing result (`asText`, `asInt`, `multi()`, etc.). Factory: `JSONataResults` — `empty`, `object`, `array`, `group`.

---

## Parse Pipeline (Infrastructure)

```
String
  → MappingExpressionsLexer
  → MappingExpressionsParser.expressions()
  → ParseTreeWalker + MappingExpressionsListener
  → List<Mapping>
```

The listener is a **compiler**: each grammar rule exit handler pushes/pops `Mapping` instances on a stack. It wires:

- Literals → constant mappings
- Paths → `MappingJoin` + `FieldMap`
- Operators → `AlgebraicOperation`, `CompareValues`, `Coalesce`, `OrderBy`, `Transform`, …
- Path binds → `PositionalBind`, `ContextBind`, `ParentReference`, `PathBindings`
- Calls → `BuiltInFunction.instantiate`, `UserDefinedFunction`, `RegisteredFunction`
- Blocks → `BlockContext` for variables and nested functions

Errors: `JSONataValidator` + `ParseCancellationException` on syntax errors.

---

## Built-in Functions

Registry: `BuiltInFunction` enum → `BuiltInSupplier` → domain `Mapping` in `functions/builtin/`.

To add a built-in:

1. Implement `Mapping` in `functions/builtin/`
2. Register in `BuiltInFunction` enum
3. Add feature tests
4. Update `DOMAIN_LANGUAGE.md` if new vocabulary

Currently registered: **66 built-ins** (string, numeric, boolean, array, object, HOF, date/time, encoding). See `BuiltInFunction` enum for the full list.

Shared helpers: `BuiltInArgs` (optional context args), `FunctionApplicator` (HOF), `PathBindings` (parent / `#` / `@`).

---

## Conformance Testing

Official [jsonata-js test suite](https://github.com/jsonata-js/jsonata/tree/master/test/test-suite) is vendored as a git submodule:

```
src/test/resources/jsonata-js/test/test-suite/
```

Initialize: `git submodule update --init --recursive`

| Component | Location |
|-----------|----------|
| Case runner | `src/test/java/dev/vepo/jsonata/conformance/ConformanceCase.java` |
| JUnit harness | `JsonataConformanceTest` (baseline report; parameterized cases `@Disabled` until pass rate improves) |
| Skip list | `ConformanceSkipList` (`performance`, `token-conversion`, `tail-recursion`) |
| Diagnostics | `ConformanceDiagnostics` — `mvn exec:java` baseline pass rate |

Current baseline: ~46% of cases pass (tracked via `printBaselineReport`).

---

## Embedding API

`EvaluationEnvironment` supports external bindings and registered functions:

```java
var env = EvaluationEnvironment.builder()
    .bind("price", mapper.readTree("{ \"foo\": { \"bar\": 45 } }"))
    .registerFunction("double", call -> ...)
    .build();
JSONata.jsonata("$price.foo.bar", env).evaluate(input);
```

`JSONata.bind()` / `registerFunction()` return new instances with merged environment (immutable-style).

---

## Module & Dependencies

**Module:** `jsonata.java` (`module-info.java`) exports `dev.vepo.jsonata` only.

| Dependency | Role |
|------------|------|
| ANTLR 4 | Lexer/parser generation and runtime |
| Jackson | JSON parsing and node construction |
| Nashorn | Regex evaluation in `RegExp` |
| SLF4J | Logging (listener, `MappingJoin`) |
| Apache Commons Text | String unescape in parser |

**Tests:** JUnit 5, AssertJ, JaCoCo (coverage → SonarCloud).

---

## Build & Quality

```bash
mvn test      # unit tests
mvn verify    # tests + JaCoCo report
mvn test -Dtest=JsonataConformanceTest#printBaselineReport  # conformance baseline
```

CI (`.github/workflows/build.yml`): JDK 21, `mvn verify` + SonarCloud analysis on every push; PRs to `main` also run the same pipeline.

---

## Extension Points

| Extension | Mechanism |
|-----------|-----------|
| New expression syntax | Edit `MappingExpressions.g4`, listener handler, domain `Mapping` |
| New built-in | `builtin/` + `BuiltInFunction` enum |
| New value shape | `Data` subtype + `JsonFactory` / `toNode` wiring |
| Public API | `JSONata`, `JSONataResult`, `EvaluationEnvironment` (module exports `dev.vepo.jsonata` only) |

---

## Design Constraints

- **Tell, don't ask** — behavior on `Data` / `Mapping`, not scattered conditionals (see `oop-principles.mdc`).
- **Thin facade** — `JSONata` orchestrates; no evaluation logic in the facade.
- **Generated code** — never edit ANTLR output under `target/`; change `.g4` and rebuild.
- **Domain language** — names and docs follow `DOMAIN_LANGUAGE.md`.

---

## When to Update This Document

Update in the **same change** when you:

- Add/remove a package or shift layer boundaries
- Change the parse → evaluate pipeline, compile/evaluate boundaries, or key abstractions
- Add a new documented violation or remove an existing one
- Add a new extension point or public API entry
- Introduce/remove a major dependency
- Change build, CI, or module exports

Do **not** update for: single-class refactors within an existing package, new built-ins that follow existing patterns, or test-only changes.
