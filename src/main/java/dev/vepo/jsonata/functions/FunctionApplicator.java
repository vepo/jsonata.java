package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Applies declared functions to elements (higher-order function support).
 */
public final class FunctionApplicator {

    private FunctionApplicator() {
    }

    public static Data apply(DeclaredFunction fn, Data original, Data current, Data element) {
        return apply(fn, original, current, element, -1, -1);
    }

    public static Data apply(DeclaredFunction fn, Data original, Data current, Data element,
                             int index, int arrayLength) {
        var names = fn.parameterNames();
        for (int i = 0; i < names.size(); i++) {
            var param = names.get(i);
            Data value = switch (i) {
                case 0 -> element;
                case 1 -> index >= 0 ? JsonFactory.numberValue(index) : current;
                case 2 -> index >= 0 ? JsonFactory.numberValue(arrayLength) : current;
                default -> current;
            };
            fn.context().defineVariable(param, (o, c) -> value);
        }
        return fn.accept(original, current, fn.context());
    }

    public static GroupedData mapArray(DeclaredFunction fn, Data original, Data current, Data array) {
        var results = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            results.add(apply(fn, original, current, array.at(i), i, array.length()));
        }
        return new GroupedData(results);
    }

    public static GroupedData filterArray(DeclaredFunction fn, Data original, Data current, Data array) {
        var results = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            var element = array.at(i);
            var test = apply(fn, original, current, element, i, array.length());
            if (test.toJson().isBoolean() && test.toJson().asBoolean()) {
                results.add(element);
            }
        }
        return new GroupedData(results);
    }

    public static Data reduceArray(DeclaredFunction fn, Data original, Data current, Data array,
                                   Data initial) {
        Data[] accumulator = { initial };
        for (int i = 0; i < array.length(); i++) {
            var element = array.at(i);
            var index = i;
            var names = fn.parameterNames();
            if (names.size() >= 2) {
                fn.context().defineVariable(names.get(0), (o, c) -> accumulator[0]);
                fn.context().defineVariable(names.get(1), (o, c) -> element);
                if (names.size() >= 3) {
                    fn.context().defineVariable(names.get(2), (o, c) -> JsonFactory.numberValue(index));
                }
                accumulator[0] = fn.accept(original, current, fn.context());
            }
        }
        return accumulator[0];
    }
}
