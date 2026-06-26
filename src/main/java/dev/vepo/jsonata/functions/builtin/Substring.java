package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Substring(List<Mapping> providers,
                        List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 3, false, original, current);
        var text = args.get(0).toJson().asText();
        var start = args.get(1).toJson().asInt();
        if (args.size() == 2) {
            return JsonFactory.stringValue(text.substring(start));
        } else {
            return JsonFactory.stringValue(text.substring(start, args.get(2).toJson().asInt()));
        }
    }
}
