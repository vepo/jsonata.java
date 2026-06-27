package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $count}. Returns the number of items in an array, or 1 for a scalar value. Uses context when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Count(List<Mapping> providers,
                    List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var data = BuiltInArgs.evaluateOne(providers, original, current);
        if (data.isArray() || data.isList()) {
            return JsonFactory.numberValue(data.length());
        } else if (!data.isEmpty()) {
            return JsonFactory.numberValue(1);
        } else {
            return JsonFactory.numberValue(0);
        }
    }
}
