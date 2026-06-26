package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Shuffle(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final Random RANDOM = new Random();

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        var array = BuiltInHelper.flattenArray(arg);
        if (!array.isArray() && !array.isList()) {
            return arg;
        }
        var result = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            result.add(array.at(i));
        }
        Collections.shuffle(result, RANDOM);
        return new ArrayData((ArrayNode) JsonFactory.arrayNode(result.stream().map(Data::toJson).toList()));
    }
}
