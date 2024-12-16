package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.json2Value;

import java.util.List;

import dev.vepo.jsonata.expression.transformers.Value;

public record InnerExpressions(List<Expression> inner) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        return json2Value(inner.stream().reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                        .map(f -> f.map(original, current)
                                                   .toJson())
                                        .orElse(current.toJson()));
    }
}