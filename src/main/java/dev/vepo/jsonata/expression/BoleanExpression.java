package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.booleanValue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.transformers.Value;

public record BoleanExpression(BooleanOperator operator, List<Expression> rightExpressions) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        return booleanValue(compare(current.toJson(),
                            rightExpressions.stream()
                                    .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                    .map(f -> f.map(original, original)
                                            .toJson())
                                    .orElse(current.toJson())));
    }

    private boolean compare(JsonNode left, JsonNode right) {
        return switch (operator) {
            case AND -> left.asBoolean() && right.asBoolean();
            case OR -> left.asBoolean() || right.asBoolean();
        };
    }

}