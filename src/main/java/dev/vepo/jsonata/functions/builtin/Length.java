package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Length(List<Mapping> providers,
                     List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    public Length {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$length function must have 1 argument!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.numberValue(providers.get(0).map(original, current).toJson().asText().length());
    }

}
