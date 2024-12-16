package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.booleanValue;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.List;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.transformers.Value;

public record CompareExpression(CompareOperator operator, List<Expression> rightExpressions) implements Expression {

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
            case EQUAL -> left.equals(right);
            case EQUAL_NOT -> !left.equals(right);
            case GREATER -> left.asInt() > right.asInt();
            case GREATER_THAN -> left.asInt() >= right.asInt();
            case LESS -> left.asInt() < right.asInt();
            case LESS_THAN -> left.asInt() <= right.asInt();
            case IN -> right.isArray() && StreamSupport.stream(spliteratorUnknownSize(right.elements(), 0), false)
                    .anyMatch(el -> el.equals(left));
        };
    }
}