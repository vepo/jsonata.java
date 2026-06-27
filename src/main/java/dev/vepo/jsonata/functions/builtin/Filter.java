package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata built-in {@code $filter}. Returns array elements for which the predicate function is truthy; uses context as the array when only a function is given.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Filter(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        Data array;
        if (providers.size() >= 2) {
            array = providers.get(0).map(original, current);
        } else {
            array = current;
        }
        if (!array.isArray() && !array.isList()) {
            return array;
        }
        if (!declaredFunctions.isEmpty()) {
            return FunctionApplicator.filterArray(declaredFunctions.get(0), original, current, array);
        }
        var callback = providers.get(providers.size() - 1);
        var results = new java.util.ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            var element = array.at(i);
            if (BuiltInHelper.toBoolean(callback.map(original, element))) {
                results.add(element);
            }
        }
        return new dev.vepo.jsonata.functions.data.GroupedData(results);
    }
}
