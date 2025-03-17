package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record LengthJSONataFunction(JSONataFunction provider) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.numberValue(provider.map(original, current).toJson().asText().length());
    }

}
