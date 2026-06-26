package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Keys(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        var json = arg.toJson();
        if (json.isObject()) {
            var keys = new ArrayList<String>();
            json.fieldNames().forEachRemaining(keys::add);
            return JsonFactory.arrayValue(keys.toArray(String[]::new));
        }
        if (arg.isArray() || arg.isList()) {
            var keys = new ArrayList<String>();
            for (int i = 0; i < arg.length(); i++) {
                keys.add(String.valueOf(i));
            }
            return JsonFactory.arrayValue(keys.toArray(String[]::new));
        }
        return Mapping.empty();
    }
}
