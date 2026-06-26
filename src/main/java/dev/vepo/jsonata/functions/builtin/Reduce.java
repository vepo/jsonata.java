package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

public record Reduce(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        Data array;
        Data initial = Mapping.empty();
        if (providers.size() >= 3) {
            array = providers.get(0).map(original, current);
            initial = providers.get(2).map(original, current);
        } else if (providers.size() == 2) {
            array = providers.get(0).map(original, current);
        } else {
            array = current;
        }
        if (!array.isArray() && !array.isList()) {
            return array;
        }
        if (!declaredFunctions.isEmpty()) {
            return FunctionApplicator.reduceArray(declaredFunctions.get(0), original, current, array, initial);
        }
        var callback = providers.get(1);
        Data accumulator = initial;
        for (int i = 0; i < array.length(); i++) {
            var element = array.at(i);
            accumulator = callback.map(original, element);
        }
        return accumulator;
    }
}
