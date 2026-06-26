package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Random(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final java.util.Random RANDOM = new java.util.Random();

    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.numberValue(RANDOM.nextDouble());
    }
}
