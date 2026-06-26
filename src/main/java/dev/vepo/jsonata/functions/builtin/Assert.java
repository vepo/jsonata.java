package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

public record Assert(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 2, false, original, current);
        if (!BuiltInHelper.toBoolean(args.get(0))) {
            var message = args.size() == 2 ? args.get(1).toJson().asText() : "Assertion failed";
            throw new JSONataException(message);
        }
        return args.get(0);
    }
}
