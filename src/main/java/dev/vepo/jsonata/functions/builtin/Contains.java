package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Contains(List<Mapping> providers,
                       List<DeclaredFunction> declaredFunctions)
        implements Mapping {
    public Contains {
        if (providers.size() != 2) {
            throw new IllegalArgumentException("$contains function must have 1 argument!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var content = providers.get(0).map(original, current).toJson().asText();
        var patternData = providers.get(1).map(original, current);
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
