package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record AlgebraicJSONataFunction(AlgebraicOperator operator, List<JSONataFunction> rightExpressions) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return numberValue(execute(current.toJson(),
                                   rightExpressions.stream()
                                                   .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                                   .map(f -> f.map(original, original)
                                                              .toJson())
                                                   .orElse(current.toJson())));
    }

    private double execute(JsonNode left, JsonNode right) {
        return switch (operator) {
            case ADD -> left.asDouble() + right.asDouble();
            case SUBTRACT -> left.asDouble() - right.asDouble();
            case MULTIPLY -> left.asDouble() * right.asDouble();
            case DIVIDE -> left.asDouble() / right.asDouble();
            case MODULO -> left.asDouble() % right.asDouble();
            case POWER -> Math.pow(left.asDouble(), right.asDouble());
        };
    }

}
