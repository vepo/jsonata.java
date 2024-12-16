package dev.vepo.jsonata.expression;

import java.util.function.Function;

import dev.vepo.jsonata.expression.transformers.Value;

public record FieldContent(Function<Value, Value> name, Function<Value, Value> value) {
}