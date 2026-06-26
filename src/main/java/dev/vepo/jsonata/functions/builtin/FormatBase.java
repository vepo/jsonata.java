package dev.vepo.jsonata.functions.builtin;

import java.math.BigInteger;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record FormatBase(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 2, false, original, current);
        var num = BuiltInHelper.toNumber(args.get(0));
        if (num == null) {
            return Mapping.empty();
        }
        var base = args.size() == 2 ? args.get(1).toJson().asInt() : 10;
        var value = num.toBigInteger();
        return JsonFactory.stringValue(value.toString(base));
    }
}
