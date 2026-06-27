package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $zip}. Combines multiple arrays element-wise into an array of tuples.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Zip(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (providers.isEmpty()) {
            return Mapping.empty();
        }
        var arrays = new ArrayList<List<Data>>();
        for (var provider : providers) {
            var data = provider.map(original, current);
            var items = new ArrayList<Data>();
            if (data.isArray() || data.isList()) {
                for (int i = 0; i < data.length(); i++) {
                    items.add(data.at(i));
                }
            } else if (!BuiltInHelper.isUndefined(data)) {
                items.add(data);
            }
            arrays.add(items);
        }
        var maxLen = arrays.stream().mapToInt(List::size).max().orElse(0);
        var result = new ArrayList<Data>();
        for (int i = 0; i < maxLen; i++) {
            var tuple = new ArrayList<Data>();
            for (var arr : arrays) {
                tuple.add(i < arr.size() ? arr.get(i) : Mapping.empty());
            }
            result.add(new ArrayData((ArrayNode) JsonFactory.arrayNode(tuple.stream().map(Data::toJson).toList())));
        }
        return new GroupedData(result);
    }
}
