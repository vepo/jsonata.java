package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record BooleanExpression(Mapping left, BooleanOperator operator, Mapping right) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return booleanValue(compare(left.map(original, current).toJson(),
                                    right.map(original, current).toJson()));
    }

    private boolean compare(JsonNode left, JsonNode right) {
        return switch (operator) {
            case AND -> left.asBoolean() && right.asBoolean();
            case OR -> left.asBoolean() || right.asBoolean();
        };
    }

}