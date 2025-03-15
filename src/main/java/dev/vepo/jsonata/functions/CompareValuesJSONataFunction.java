package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record CompareValuesJSONataFunction(JSONataFunction left, CompareOperator operator, JSONataFunction right) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return booleanValue(compare(left.map(original, current).toJson(),
                                    right.map(original, original).toJson()));
    }

    private boolean compare(JsonNode left, JsonNode right) {
        return switch (operator) {
            case EQUAL -> left.equals(right);
            case EQUAL_NOT -> !left.equals(right);
            case GREATER -> left.asLong() > right.asLong();
            case GREATER_THAN -> left.asLong() >= right.asLong();
            case LESS -> left.asLong() < right.asLong();
            case LESS_THAN -> left.asLong() <= right.asLong();
            case IN -> right.isArray() && StreamSupport.stream(spliteratorUnknownSize(right.elements(), 0), false)
                                                       .anyMatch(el -> el.equals(left));
        };

    }
}