package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $substringAfter}. Returns the portion of a string after the first occurrence of a pattern.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record SubstringAfter(List<Mapping> providers,
                             List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 2, false, original, current);
        var value = args.get(0).toJson().asText();
        var pattern = args.get(1).toJson().asText();
        var index = value.indexOf(pattern);
        return JsonFactory.stringValue(index >= 0 ? value.substring(index + pattern.length()) : value);
    }
}
