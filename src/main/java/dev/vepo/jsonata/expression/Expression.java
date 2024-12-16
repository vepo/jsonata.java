package dev.vepo.jsonata.expression;

import dev.vepo.jsonata.expression.transformers.Value;

@FunctionalInterface
public interface Expression {
    Value map(Value original, Value current);
}
