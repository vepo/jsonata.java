package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

public record Error(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var message = BuiltInArgs.evaluateOne(providers, original, current).toJson().asText();
        throw new JSONataException(message);
    }
}
