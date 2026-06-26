# JSONata.java

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=vepo_jsonata.java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=vepo_jsonata.java)

A native [JSONata](https://docs.jsonata.org/) implementation for Java 21. Parse expressions once, evaluate them against JSON payloads, and get typed results — no embedded JavaScript engine required.

## Why JSONata?

JSON is everywhere, but working with nested or variable-shaped documents in Java often means verbose Jackson code: loops, null checks, intermediate DTOs, and glue that obscures the actual transformation.

**JSONata** is a declarative query and transformation language for JSON. Think XPath for JSON: you describe *what* you want from the data, not *how* to walk the tree.

| Without JSONata | With JSONata |
|-----------------|--------------|
| Write navigation, filtering, and mapping in Java | Express the same logic in one compact expression |
| Change the output shape → refactor Java code | Change the expression string |
| Reuse logic across services → share Java classes | Reuse the same expression in Java, config files, or other JSONata runtimes |

### Good fits

- **API integration** — reshape vendor JSON into your domain model
- **Event processing** — extract fields, filter arrays, compute aggregates from payloads
- **Configuration and rules** — keep transformation logic in data-driven expressions instead of hard-coded Java
- **ETL and reporting** — project, group, and summarize JSON documents without a separate tool chain

### Why this library?

- **Native Java** — ANTLR parser and Java evaluator; no Node.js or Nashorn for core evaluation
- **Familiar API** — compile with `JSONata.jsonata(expr)`, evaluate with `.evaluate(json)`
- **Extensible** — bind variables and register custom functions from Java
- **Spec-aligned** — validated against the [jsonata-js test suite](https://github.com/jsonata-js/jsonata)

## Requirements

- Java 21+
- Maven 3.x (to build from source)

## Installation

The library is not yet published to Maven Central. Build and install locally:

```bash
git clone https://github.com/vepo/jsonata.java.git
cd jsonata.java
git submodule update --init --recursive
mvn install
```

Then add the dependency to your project:

```xml
<dependency>
    <groupId>dev.vepo</groupId>
    <artifactId>jsonata.java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The module name is `jsonata.java` (JPMS). Only `dev.vepo.jsonata` is exported.

## How to use

### Basic evaluation

The typical workflow is **compile once, evaluate many times**:

```java
import static dev.vepo.jsonata.JSONata.jsonata;

var expression = jsonata("Address.City");
var result = expression.evaluate("""
    {
        "FirstName": "Fred",
        "Address": { "City": "Winchester" }
    }
    """);

result.asText(); // "Winchester"
```

`JSONata.jsonata(String)` parses the expression and returns a reusable evaluator. Pass a JSON string to `evaluate`; the root document becomes the expression context (`$`).

### Reading results

`evaluate` returns a `JSONataResult`. Use typed accessors for scalar values:

```java
result.asText();     // string
result.asInt();      // integer
result.asDouble();   // floating point
result.asBoolean();  // boolean
result.isNull();     // JSON null
result.isEmpty();    // no match (JSONata "undefined")
```

When an expression produces a sequence (for example, filtering an array), use `multi()`:

```java
var emails = jsonata("Phone[type='mobile'].number").evaluate(json);

emails.multi().asText(); // List.of("077 7700 1234")
```

For complex output (objects, arrays), `asText()` returns a JSON string:

```java
var grouped = jsonata("""
    Account.Order.Product {
        `Product Name`: $sum($.(Price * Quantity))
    }
    """).evaluate(invoiceJson);

grouped.asText(); // {"Bowler Hat":206.7,"Trilby hat":21.67,...}
```

### Expression examples

These illustrate common patterns; the full language is documented at [docs.jsonata.org](https://docs.jsonata.org/).

**Field access and navigation**

```java
jsonata("Surname").evaluate(person).asText();
jsonata("Address.City").evaluate(person).asText();
jsonata("Other.Nothing").evaluate(person).isEmpty(); // path not found
```

**Filtering and predicates**

```java
jsonata("Phone[type='office'].number").evaluate(person).multi().asText();
jsonata("Numbers[>$average(Numbers)]").evaluate(data).multi().asInt();
```

**String and numeric expressions**

```java
jsonata("FirstName & ' ' & Surname").evaluate(person).asText();
jsonata("$sum(Account.Order.Product.(Price * Quantity))").evaluate(invoice).asDouble();
```

**Constructing output**

```java
jsonata("""
    Account.Order.Product.{
        "name": `Product Name`,
        "total": Price * Quantity
    }
    """).evaluate(invoice).asText();
```

Try expressions interactively at [try.jsonata.org](https://try.jsonata.org/).

### Variable bindings

Pass external values into an expression with `$variable` names. Bindings must be registered **before** parsing:

```java
import dev.vepo.jsonata.EvaluationEnvironment;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

var env = EvaluationEnvironment.builder()
    .bind("threshold", JsonNodeFactory.instance.numberNode(100))
    .build();

var expr = jsonata("Numbers[$ > $threshold]", env);
expr.evaluate("{\"Numbers\":[1,50,150,200]}").multi().asInt(); // [150, 200]
```

You can also chain bindings on a compiled expression (returns a new instance):

```java
var expr = jsonata("$greeting & ' ' & Surname")
    .bind("greeting", "\"Hello\"");
```

### Custom functions

Register Java functions that JSONata expressions can call. Like bindings, functions must exist at parse time:

```java
var env = EvaluationEnvironment.builder()
    .registerFunction("add", call -> {
        var left = call.arguments().get(0).map(call.original(), call.current());
        var right = call.arguments().get(1).map(call.original(), call.current());
        var sum = left.toJson().asInt() + right.toJson().asInt();
        return dev.vepo.jsonata.functions.data.Data.load(String.valueOf(sum));
    })
    .build();

jsonata("$add(10, 32)", env).evaluate("{}").asInt(); // 42
```

Each `MappingCall` provides:

- `original()` — the root input document
- `current()` — the context node where the function was invoked
- `arguments()` — unevaluated argument expressions (evaluate with `.map(original, current)`)

### Error handling

| Situation | Exception |
|-----------|-----------|
| Invalid JSON input | `JSONataException` — `"Invalid JSON! content=..."` |
| Invalid expression syntax | `ParseCancellationException` (ANTLR) |
| Unknown function at parse time | `JSONataException` — `"Function not found: $name"` |
| Runtime evaluation errors | `JSONataException` or `IllegalArgumentException` |

Parse expressions inside a `try/catch` when loading user-supplied expression text; validate JSON input before evaluation in untrusted paths.

### Thread safety

Compiled `JSONata` instances are safe to reuse across threads. Each `evaluate` call clears internal path bindings before and after execution. For high concurrency, prefer one shared compiled expression over parsing per request.

## Development

```bash
mvn test                                                          # unit tests
mvn verify                                                        # tests + JaCoCo coverage
mvn test -Dtest=JsonataConformanceTest#printBaselineReport        # conformance pass rate
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for internal design and [AGENTS.md](AGENTS.md) for contributor workflows.

## References

- [JSONata language documentation](https://docs.jsonata.org/)
- [JSONata overview](https://docs.jsonata.org/overview)
- [Try JSONata online](https://try.jsonata.org/)
- [jsonata-js reference implementation](https://github.com/jsonata-js/jsonata)

## License

Apache License 2.0 — see [LICENSE](LICENSE).
