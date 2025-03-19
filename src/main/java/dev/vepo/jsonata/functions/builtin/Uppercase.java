package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Uppercase(List<Mapping> providers,
                        List<DeclaredFunction> declaredFunctions)
        implements Mapping {
    public Uppercase {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$uppercase function must have 1 argument!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var previusData = providers.get(0).map(original, current);
        return previusData.map(node -> JsonFactory.stringValue(node.asText().toUpperCase()));
    }
}
