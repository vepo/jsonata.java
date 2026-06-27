package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $contains}. Returns whether a string contains a substring or matches a regular expression.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Contains(List<Mapping> providers,
                       List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    /** {@inheritDoc} */
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
