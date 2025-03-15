package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.json2Value;

import dev.vepo.jsonata.functions.data.Data;

public record ContextValueJSONataFunction(JSONataFunction inner) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return json2Value(inner.map(current, current).toJson());
    }
}