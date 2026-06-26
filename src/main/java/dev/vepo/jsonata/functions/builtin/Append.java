package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Append(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 2, false, original, current);
        var array = BuiltInHelper.flattenArray(args.get(0));
        if (!array.isArray()) {
            return Mapping.empty();
        }
        var result = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            result.add(array.at(i));
        }
        if (args.size() == 2) {
            var extra = args.get(1);
            if (extra.isArray() || extra.isList()) {
                for (int i = 0; i < extra.length(); i++) {
                    result.add(extra.at(i));
                }
            } else if (!BuiltInHelper.isUndefined(extra)) {
                result.add(extra);
            }
        }
        return new ArrayData((ArrayNode) JsonFactory.arrayNode(result.stream().map(Data::toJson).toList()));
    }
}
