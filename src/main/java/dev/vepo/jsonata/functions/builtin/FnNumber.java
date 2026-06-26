package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record FnNumber(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        var json = arg.toJson();
        if (json.isNumber()) {
            return arg;
        }
        if (json.isBoolean()) {
            return JsonFactory.numberValue(json.asBoolean() ? 1 : 0);
        }
        if (json.isTextual()) {
            var num = BuiltInHelper.toNumber(arg);
            return num != null ? JsonFactory.numberValue(num) : Mapping.empty();
        }
        return Mapping.empty();
    }
}
