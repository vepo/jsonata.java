package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata built-in {@code $single}. Returns the sole matching element or throws when zero or multiple matches exist.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Single(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

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
        Data result = Mapping.empty();
        int count = 0;
        for (int i = 0; i < array.length(); i++) {
            var element = array.at(i);
            boolean match;
            if (!declaredFunctions.isEmpty()) {
                var test = FunctionApplicator.apply(declaredFunctions.get(0), original, current, element, i,
                        array.length());
                match = BuiltInHelper.toBoolean(test);
            } else {
                match = BuiltInHelper.toBoolean(providers.get(providers.size() - 1).map(original, element));
            }
            if (match) {
                count++;
                result = element;
            }
        }
        if (count == 1) {
            return result;
        }
        if (count == 0) {
            return Mapping.empty();
        }
        throw new JSONataException("The $single() function expected exactly 1 matching result.  Instead it matched " + count);
    }
}
