package dev.vepo.jsonata.expression.transformers;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.fromString;
import static java.util.Objects.requireNonNull;

import java.util.List;

import dev.vepo.jsonata.expression.Expression;
import dev.vepo.jsonata.expression.Node;

public record JsonValue(Value actual) {

    public JsonValue(String value) {
        this(fromString(value));
    }

    public Node apply(List<Expression> expressions) {
        requireNonNull(expressions, "Expressions cannot be null!");
        return expressions.stream()
                          .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                          .map(f -> f.map(actual, actual)
                                     .toNode())
                          .orElse(actual.toNode());
    }

}
