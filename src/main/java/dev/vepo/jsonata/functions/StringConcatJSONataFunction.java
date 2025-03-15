package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

public record StringConcatJSONataFunction(JSONataFunction firstValue, JSONataFunction secondValue) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return stringValue(safeAsText(firstValue.map(original, current)) + safeAsText(secondValue.map(original, current)));
    }

    private static String safeAsText(Data value) {
        return Optional.ofNullable(value.toJson())
                       .map(JsonNode::asText)
                       .orElse("");
    }
}