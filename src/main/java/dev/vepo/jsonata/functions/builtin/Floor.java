package dev.vepo.jsonata.functions.builtin;

import java.math.RoundingMode;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Floor(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        var num = BuiltInHelper.toNumber(arg);
        return num != null ? JsonFactory.numberValue(num.setScale(0, RoundingMode.FLOOR)) : Mapping.empty();
    }
}
