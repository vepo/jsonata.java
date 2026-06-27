package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $sift}. Returns a new object containing only properties for which the predicate function is truthy.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Sift(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (providers.size() < 2 || declaredFunctions.isEmpty()) {
            throw new IllegalArgumentException("$sift requires an object and a predicate function");
        }
        var object = providers.get(0).map(original, current);
        if (!object.toJson().isObject()) {
            return Mapping.empty();
        }
        var fn = declaredFunctions.get(0);
        var builder = JsonFactory.objectBuilder();
        object.toJson().fields().forEachRemaining(entry -> {
            var key = entry.getKey();
            var value = JsonFactory.json2Value(entry.getValue());
            fn.context().defineVariable(fn.parameterNames().get(0), (o, c) -> value);
            if (fn.parameterNames().size() > 1) {
                fn.context().defineVariable(fn.parameterNames().get(1), (o, c) -> JsonFactory.stringValue(key));
            }
            var test = fn.accept(original, current, fn.context());
            if (BuiltInHelper.toBoolean(test)) {
                builder.set(key, value);
            }
        });
        return builder.build();
    }
}
