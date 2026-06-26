package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Power(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 2, false, original, current);
        var base = BuiltInHelper.toNumber(args.get(0));
        if (base == null) {
            return Mapping.empty();
        }
        var exponent = args.size() == 2 ? BuiltInHelper.toNumber(args.get(1)) : base;
        if (exponent == null) {
            return Mapping.empty();
        }
        return JsonFactory.numberValue(Math.pow(base.doubleValue(), exponent.doubleValue()));
    }
}
