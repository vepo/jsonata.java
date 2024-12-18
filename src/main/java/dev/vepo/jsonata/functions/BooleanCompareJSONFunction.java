package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record BooleanCompareJSONFunction(BooleanOperator operator, List<JSONataFunction> rightExpressions) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
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