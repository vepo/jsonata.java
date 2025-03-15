package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;

import dev.vepo.jsonata.functions.data.Data;

public record StringConcatJSONataFunction(JSONataFunction firstValue, JSONataFunction secondValue) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return stringValue(firstValue.map(original, current).toJson().asText() +
                secondValue.map(original, current).toJson().asText());
    }
}