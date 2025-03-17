package dev.vepo.jsonata.functions.buildin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record SubstringAfterJSONataFunction(List<JSONataFunction> providers,
                                            List<DeclaredFunction> declaredFunctions)
        implements JSONataFunction {
    public SubstringAfterJSONataFunction {
        if (providers.size() != 2) {
            throw new IllegalArgumentException("$substringAfter function must have 2 arguments");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var value = providers.get(0).map(original, current).toJson().asText();
        var pattern = providers.get(1).map(original, current).toJson().asText();
        return JsonFactory.stringValue(value.substring(value.indexOf(pattern) + pattern.length()));
    }

}
