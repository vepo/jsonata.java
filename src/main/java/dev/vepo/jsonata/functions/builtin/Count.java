package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Count(List<Mapping> providers,
                    List<DeclaredFunction> declaredFunctions)
        implements Mapping {
    public Count {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$count function must have 1 parameter!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var data = providers.get(0).map(original, current);
        if (data.isArray() || data.isList()) {
            return JsonFactory.numberValue(data.length());
        } else if (!data.isEmpty()) {
            return JsonFactory.numberValue(1);
        } else {
            return JsonFactory.numberValue(0);
        }
    }

}
