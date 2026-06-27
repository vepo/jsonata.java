package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.FunctionApplicator;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $each}. Applies a function to each entry of an array or object and returns the results as an array.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Each(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (providers.size() < 2 || declaredFunctions.isEmpty()) {
            throw new IllegalArgumentException("$each requires a value and a function");
        }
        var value = providers.get(0).map(original, current);
        var fn = declaredFunctions.get(0);
        var results = new ArrayList<Data>();
        if (value.isArray() || value.isList()) {
            for (int i = 0; i < value.length(); i++) {
                results.add(FunctionApplicator.apply(fn, original, current, value.at(i), i, value.length()));
            }
        } else if (value.toJson().isObject()) {
            value.toJson().fields().forEachRemaining(entry -> {
                var fieldValue = JsonFactory.json2Value(entry.getValue());
                fn.context().defineVariable(fn.parameterNames().get(0), (o, c) -> fieldValue);
                if (fn.parameterNames().size() > 1) {
                    fn.context().defineVariable(fn.parameterNames().get(1), (o, c) -> JsonFactory.stringValue(entry.getKey()));
                }
                results.add(fn.accept(original, current, fn.context()));
            });
        } else if (!BuiltInHelper.isUndefined(value)) {
            results.add(FunctionApplicator.apply(fn, original, current, value));
        }
        return new GroupedData(results);
    }
}
