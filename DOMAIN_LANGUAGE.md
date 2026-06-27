# Domain Language — JSONata.java

**Canonical glossary for this codebase.** Agents must read this before domain work and update it when introducing or changing domain concepts.

Reference: [JSONata documentation](https://docs.jsonata.org)

---

## Core Concepts

| Term | Meaning | Code anchor |
|------|---------|-------------|
| **Expression** | A JSONata query or transformation written as text | Parsed by `MappingParser` |
| **Mapping** | Evaluable expression logic; transforms input **context** into **data** | `Mapping` |
| **Evaluate** | Run parsed expression(s) against JSON input | `JSONata.evaluate` |
| **Context** | Current value during evaluation (`$` in JSONata) | Second arg to `Mapping.map` |
| **Root input** | Original JSON document at evaluation start (`$$` in JSONata) | First arg to `Mapping.map` |
| **Data** | Domain representation of a JSON value during evaluation | `Data` and subtypes |
| **Result** | Outcome exposed to callers after evaluation | `JSONataResult` |
| **Empty result** | No match / undefined sequence (not JSON `null`) | `EmptyData`, `JSONataEmptyResult` |

## Expression Constructs

| Term | Meaning | Code anchor |
|------|---------|-------------|
| **Path** | Navigate fields with `.` (map over context) | `MappingJoin`, `FieldMap` |
| **Predicate** | Filter with `[condition]` or index with `[n]` | `ArrayQuery`, `ArrayIndex` |
| **Wildcard** | Select all properties at one level (`*`) | `Wildcard` |
| **Descendant search** | Recursive property search (`**`) | `DeepFind` |
| **Block** | Grouped statements in `( … )` | `BlockContext` |
| **Variable** | Named binding with `:=` | `BlockContext.defineVariable` |
| **Built-in function** | Library function prefixed with `$` | `BuiltInFunction`, `builtin/` |
| **User-defined function** | Lambda declared with `function(…) { … }` | `DeclaredFunction`, `UserDefinedFunction` |
| **Chain** | Pass result to next step with `~>` | `ChainExpression`, `ChainMapping` |
| **Coalesce** | Default when empty with `??` | `Coalesce` |
| **Order-by** | Sort with `^(expr)` | `OrderBy`, `OrderKey` |
| **Parent reference** | Enclosing object with `%` | `ParentReference`, `PathBindings` |
| **Positional bind** | Index variable with `#$var` | `PositionalBind` |
| **Context bind** | Focus variable with `@$var` | `ContextBind` |
| **Transform** | Object update via `\|path\|update\|` (copy, then merge/delete matched nodes) | `Transform` |
| **Data inspector** | Jackson-free domain port for copy, merge, remove, and functional replace on `Data` | `DataInspector` |
| **Evaluation context** | Thread-local session holding the active `DataInspector` during evaluate | `EvaluationContext` |
| **Data backing** | Underlying representation of a `Data` value (e.g. Jackson `JsonNode`, map, POJO) | `Data.inspector()` |
| **Default data inspector** | Registry for the fallback inspector when no session is active | `DataInspectors` |
| **Binding** | External variable at evaluate time | `EvaluationEnvironment` |
| **Group / reduce** | Aggregate into object with `{ key: value }` | `AggregateMapping`, `JSONataGroupResult` |
| **Object constructor** | Build object `{ "field": expr }` | `ObjectBuilder` |
| **Array constructor** | Build array `[ expr, … ]` | `ArrayConstructor` |

## Data Shapes

| Term | Meaning | Code anchor |
|------|---------|-------------|
| **Object data** | JSON object value | `ObjectData` |
| **Array data** | JSON array value | `ArrayData` |
| **Grouped data** | Sequence of values from multi-match navigation | `GroupedData` |
| **Regex value** | Regular expression literal | `RegexData`, `RegExp` |
| **Jackson data inspector** | Default mutable `DataInspector` adapter for Jackson-backed `Data` | `MutableJacksonDataInspector` |
| **Immutable Jackson data inspector** | Functional-update `DataInspector` for immutable Jackson-backed `Data` | `ImmutableJacksonDataInspector` |

## DataInspector contract

`DataInspector` is a **domain port** (JDK + `Data` only). It does **not** load JSON or reference Jackson types. Data creation stays in `JsonFactory` (infrastructure), which tags new `Data` with `EvaluationContext.currentInspector()`.

| Method | Mutable inspector | Immutable inspector |
|--------|-------------------|---------------------|
| `mutableValues()` | `true` | `false` |
| `copy` | Deep working copy | Deep copy (new `Data` instances) |
| `mergeFields` / `removeFields` | In-place update | Throws `JSONataException` |
| `merged` / `withoutFields` / `replaceNode` | Delegates to in-place ops | Builds new `Data`; `replaceNode` re-roots updated subtrees |

Session wiring: `JSONata.jsonata(expr, inspector)` → `EvaluationEnvironment.withDataInspector` → `EvaluationContext.call` during `evaluate` / `evaluateData`.

## Layers (Bounded Context)

| Layer | Responsibility | Packages |
|-------|----------------|----------|
| **Application** | Public API; orchestrates evaluation | `dev.vepo.jsonata` |
| **Domain** | Expression semantics and value model | `functions/`, `results/`, `exception/` |
| **Infrastructure** | Parsing, JSON I/O, regex engine | `parser/`, `functions/json/`, `functions/regex/` |

## Naming Rules

- Types, methods, and tests use terms from this document or the JSONata spec.
- Do **not** name domain concepts after ANTLR rules, listener methods, or Jackson types.
- When a spec term and a Java type differ, note the mapping here (see table above).

## When to Update This Document

Update in the **same change** when you:

- Add a new domain concept, built-in function category, or `Data` subtype
- Introduce a JSONata language construct not yet listed
- Rename a domain type or change its meaning
- Discover a spec term that maps to an existing type (add to table)
- Add or change `DataInspector` methods or evaluation-scoped wiring terms

See `.cursor/rules/documentation-workflow.mdc` for the full documentation checklist.

Do **not** update for pure refactors that preserve meaning, or infrastructure-only changes with no new domain vocabulary.
