package dev.vepo.jsonata.functions.buildin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record TrimJSONataFunction(List<JSONataFunction> providers,
                                  List<DeclaredFunction> declaredFunctions)
        implements JSONataFunction {
    public TrimJSONataFunction {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$trim function must have 1 argument");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.stringValue(providers.get(0).map(original, current).toJson().asText().trim());
    }

}
