package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Length(List<Mapping> providers,
                     List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        var json = arg.toJson();
        if (json.isTextual()) {
            return JsonFactory.numberValue(json.asText().codePointCount(0, json.asText().length()));
        }
        if (arg.isArray() || arg.isList()) {
            return JsonFactory.numberValue(arg.length());
        }
        return Mapping.empty();
    }
}
