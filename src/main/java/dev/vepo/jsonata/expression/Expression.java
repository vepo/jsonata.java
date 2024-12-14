package dev.vepo.jsonata.expression;

import dev.vepo.jsonata.expression.JsonValue.Value;

@FunctionalInterface
public interface Expression {
    Value map(Value node);
}
