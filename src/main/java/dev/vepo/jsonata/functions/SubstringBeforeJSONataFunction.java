package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record SubstringBeforeJSONataFunction(List<JSONataFunction> valueProviders) implements JSONataFunction {
    public SubstringBeforeJSONataFunction {
        if (valueProviders.size() != 2) {
            throw new IllegalArgumentException("SubstringBefore function must have 2 arguments");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var value = valueProviders.get(0).map(original, current).toJson().asText();
        var pattern = valueProviders.get(1).map(original, current).toJson().asText();
        return JsonFactory.stringValue(value.substring(0, value.indexOf(pattern)));
    }

}
