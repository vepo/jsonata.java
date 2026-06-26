package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;
import dev.vepo.jsonata.functions.regex.RegExp;

public record Replace(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 3, 4, false, original, current);
        var content = args.get(0).toJson().asText();
        var patternData = args.get(1);
        if (!patternData.isRegex()) {
            return JsonFactory.stringValue(content);
        }
        var regex = patternData.asRegex();
        Integer limit = args.size() == 4 ? args.get(3).toJson().asInt() : -1;
        if (!declaredFunctions.isEmpty()) {
            var fn = declaredFunctions.get(0);
            return JsonFactory.stringValue(regex.replace(content, match -> {
                var matchObj = buildMatchObject(match);
                return FunctionApplicator.apply(fn, original, current, matchObj).toJson().asText();
            }, limit));
        }
        var replacement = args.get(2).toJson().asText();
        return JsonFactory.stringValue(regex.replace(content, replacement, limit));
    }

    private static Data buildMatchObject(RegExp.MatchResult match) {
        var builder = JsonFactory.objectBuilder();
        builder.set("match", JsonFactory.stringValue(match.match()));
        builder.set("index", JsonFactory.numberValue(match.index()));
        if (!match.groups().isEmpty()) {
            builder.set("groups", JsonFactory.arrayValue(match.groups().toArray(String[]::new)));
        }
        return builder.build();
    }
}
