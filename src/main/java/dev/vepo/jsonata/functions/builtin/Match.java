package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Match(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 2, false, original, current);
        var content = args.get(0).toJson().asText();
        var patternData = args.get(1);
        if (!patternData.isRegex()) {
            return Mapping.empty();
        }
        var match = patternData.asRegex().match(content);
        if (match == null) {
            return Mapping.empty();
        }
        var builder = JsonFactory.objectBuilder();
        builder.set("match", JsonFactory.stringValue(match.match()));
        builder.set("index", JsonFactory.numberValue(match.index()));
        if (!match.groups().isEmpty()) {
            builder.set("groups", JsonFactory.arrayValue(match.groups().toArray(String[]::new)));
        }
        return builder.build();
    }
}
