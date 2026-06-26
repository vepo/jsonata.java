package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.ObjectData;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Merge(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

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
