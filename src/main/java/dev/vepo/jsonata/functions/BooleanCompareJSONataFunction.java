package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record BooleanCompareJSONataFunction(BooleanOperator operator, JSONataFunction rightExpressions) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return booleanValue(compare(current.toJson(),
                                    rightExpressions.map(original, current).toJson()));
    }

    private boolean compare(JsonNode left, JsonNode right) {
        return switch (operator) {
            case AND -> left.asBoolean() && right.asBoolean();
            case OR -> left.asBoolean() || right.asBoolean();
        };
    }

}