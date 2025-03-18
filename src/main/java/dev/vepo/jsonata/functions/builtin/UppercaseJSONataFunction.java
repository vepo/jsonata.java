package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record UppercaseJSONataFunction(List<JSONataFunction> providers,
                                       List<DeclaredFunction> declaredFunctions)
        implements JSONataFunction {
    public UppercaseJSONataFunction {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$uppercase function must have 1 argument!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.stringValue(providers.get(0).map(original, current).toJson().asText().toUpperCase());
    }
}
