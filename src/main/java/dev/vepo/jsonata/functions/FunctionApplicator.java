package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        if (names.isEmpty()) {
            return FunctionApplyService.applyDeclared(fn, original, current, List.of(), Optional.empty());
        }
        var args = new ArrayList<Data>();
        args.add(element);
        if (names.size() >= 2 && index >= 0) {
            args.add(JsonFactory.numberValue(index));
        }
        if (names.size() >= 3 && index >= 0) {
            args.add(JsonFactory.numberValue(arrayLength));
        }
        var argProviders = args.stream().<Mapping>map(a -> (o, c) -> a).toList();
        return FunctionApplyService.applyDeclared(fn, original, current, argProviders, Optional.empty());
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
                var frame = fn.closureContext().createChildFrame();
                frame.defineVariable(names.get(0), (o, c) -> accumulator[0]);
                frame.defineVariable(names.get(1), (o, c) -> element);
                if (names.size() >= 3) {
                    frame.defineVariable(names.get(2), (o, c) -> JsonFactory.numberValue(index));
                }
                accumulator[0] = fn.accept(original, current, frame);
            }
        }
        return accumulator[0];
    }
}
