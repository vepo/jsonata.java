package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $merge}. Deep-merges one or more objects into a single object.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Merge(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (providers.isEmpty()) {
            return Mapping.empty();
        }
        var builder = JsonFactory.objectBuilder();
        for (var provider : providers) {
            var data = provider.map(original, current);
            if (data.isArray() || data.isList()) {
                for (int i = 0; i < data.length(); i++) {
                    mergeObject(builder, data.at(i));
                }
            } else {
                mergeObject(builder, data);
            }
        }
        return builder.build();
    }

    private static void mergeObject(JsonFactory.ObjectBuilder builder, Data data) {
        if (data.toJson() != null && data.toJson().isObject()) {
            data.toJson().fields().forEachRemaining(entry -> {
                var key = entry.getKey();
                var value = JsonFactory.json2Value(entry.getValue());
                if (builder.hasValue(key) && entry.getValue().isObject()) {
                    builder.set(key, value, true);
                } else {
                    builder.set(key, value);
                }
            });
        }
    }
}
