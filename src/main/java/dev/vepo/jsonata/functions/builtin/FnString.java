package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record FnString(List<Mapping> providers,
                       List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        return JsonFactory.stringValue(arg.toJson().asText());
    }
}
