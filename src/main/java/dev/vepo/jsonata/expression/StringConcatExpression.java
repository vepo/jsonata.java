package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.stringValue;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.Function;

import dev.vepo.jsonata.expression.transformers.Value;

public record StringConcatExpression(List<Function<Value, Value>> sources) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        return stringValue(sources.stream()
                .map(fn -> fn.apply(current).toJson().asText())
                .collect(joining()));
    }
}