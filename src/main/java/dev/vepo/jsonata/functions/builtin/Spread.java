package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $spread}. Expands an array of objects or a single object for use in object merge/append contexts.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Spread(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        if (arg.isArray() || arg.isList()) {
            var results = new ArrayList<Data>();
            for (int i = 0; i < arg.length(); i++) {
                var element = arg.at(i);
                if (element.toJson().isObject()) {
                    results.add(element);
                }
            }
            return new GroupedData(results);
        }
        if (arg.toJson().isObject()) {
            return arg;
        }
        return Mapping.empty();
    }
}
