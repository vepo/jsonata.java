package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Contains(List<Mapping> providers,
                       List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 2, false, original, current);
        var content = args.get(0).toJson().asText();
        var patternData = args.get(1);
        if (patternData.isRegex()) {
            return JsonFactory.booleanValue(patternData.asRegex().isContainedIn(content));
        } else {
            var pattern = patternData.toJson();
            if (pattern.isTextual()) {
                return JsonFactory.booleanValue(content.contains(pattern.asText()));
            } else {
                throw new IllegalStateException("Cannot execute $contains. pattern=" + pattern);
            }
        }
    }
}
