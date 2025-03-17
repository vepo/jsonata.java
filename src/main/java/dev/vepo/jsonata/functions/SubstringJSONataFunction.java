package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record SubstringJSONataFunction(List<JSONataFunction> providers) implements JSONataFunction {
    public SubstringJSONataFunction {
        if (providers.size() < 2 || providers.size() > 3) {
            throw new IllegalArgumentException("Substring function must have 2 or 3 parameters");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        if (providers.size() == 2) {
            return JsonFactory.stringValue(providers.get(0).map(original, current).toJson().asText()
                                                    .substring(providers.get(1).map(original, current).toJson().asInt()));
        } else {
            return JsonFactory.stringValue(providers.get(0).map(original, current).toJson().asText()
                                                    .substring(providers.get(1).map(original, current).toJson().asInt(),
                                                               providers.get(2).map(original, current).toJson().asInt()));
        }
    }

}
