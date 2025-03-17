package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record LengthJSONataFunction(List<JSONataFunction> providers) implements JSONataFunction {

    public LengthJSONataFunction {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("Length function must have 1 argument");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.numberValue(providers.get(0).map(original, current).toJson().asText().length());
    }

}
