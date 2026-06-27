package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

/**
 * JSONata built-in {@code $map}. Applies a function to each element of an array; uses context as the array when only a function is given.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record MapFn(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

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
            return FunctionApplicator.mapArray(declaredFunctions.get(0), original, current, array);
        }
        var callback = providers.get(providers.size() - 1);
        var results = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            results.add(callback.map(original, array.at(i)));
        }
        return new GroupedData(results);
    }
}
