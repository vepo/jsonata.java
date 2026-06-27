package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Applies declared functions to individual elements (higher-order function support).
 *
 * <p>Used by built-ins such as {@code $map}, {@code $filter}, and {@code $reduce} to
 * invoke block-scoped or anonymous functions with the correct positional/index/length
 * arguments per the JSONata function signature conventions.
 */
public final class FunctionApplicator {

    private FunctionApplicator() {
    }

    /**
     * Applies a function to a single element with no index or length arguments.
     *
     * @param fn       the declared function
     * @param original root input document
     * @param current  current focus
     * @param element  the element to pass as the first argument
     * @return the function result for {@code element}
     */
    public static Data apply(DeclaredFunction fn, Data original, Data current, Data element) {
        return apply(fn, original, current, element, -1, -1);
    }

    /**
     * Applies a function to a single element, supplying index and array length when the
     * function signature expects them (2- or 3-parameter forms).
     *
     * @param fn          the declared function
     * @param original    root input document
     * @param current     current focus
     * @param element     the element to pass as the first argument
     * @param index       zero-based index, or {@code -1} to omit
     * @param arrayLength total array length, or {@code -1} to omit
     * @return the function result for {@code element}
     */
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

    /**
     * Maps a function over every element of an array, returning a grouped sequence.
     *
     * @param fn       the declared function
     * @param original root input document
     * @param current  current focus
     * @param array    the array to map over
     * @return a {@link GroupedData} of mapped results
     */
    public static GroupedData mapArray(DeclaredFunction fn, Data original, Data current, Data array) {
        var results = new ArrayList<Data>();
        for (int i = 0; i < array.length(); i++) {
            results.add(apply(fn, original, current, array.at(i), i, array.length()));
        }
        return new GroupedData(results);
    }

    /**
     * Filters an array, retaining elements for which the predicate function returns true.
     *
     * @param fn       the declared predicate function
     * @param original root input document
     * @param current  current focus
     * @param array    the array to filter
     * @return a {@link GroupedData} of matching elements (original values, not mapped)
     */
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

    /**
     * Reduces an array left-to-right using a declared function and initial accumulator.
     *
     * @param fn       the declared reducer (accumulator, item[, index])
     * @param original root input document
     * @param current  current focus
     * @param array    the array to reduce
     * @param initial  the starting accumulator value
     * @return the final accumulator
     */
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
