package dev.vepo.jsonata.functions;

import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ContextValueJSONataFunction(JSONataFunction inner) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return Optional.ofNullable(inner.map(current, current))
                       .map(Data::toJson)
                       .map(JsonFactory::json2Value)
                       .orElseGet(JSONataFunction::empty);
    }
}