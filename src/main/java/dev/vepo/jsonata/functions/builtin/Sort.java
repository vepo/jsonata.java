package dev.vepo.jsonata.functions.builtin;

import java.util.List;
import java.util.stream.IntStream;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.PathBindings;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

/**
 * JSONata built-in {@code $sort}. Returns a sorted copy of an array, optionally using a comparator function.
 *
 * @param providers argument expression mappings from the parse tree
 * @param function optional comparator function from the parse tree
 * @param comparator resolved comparator used during sorting
 */
public record Sort(List<Mapping> providers, List<DeclaredFunction> function,
                   SortComparator comparator)
        implements Mapping {

    /**
     * Compares two array elements during {@code $sort}.
     */
    @FunctionalInterface
    public interface SortComparator {
        /**
         * Compares two array elements for ordering.
         *
         * @param original root input data for the expression
         * @param current evaluation context data
         * @param left first element
         * @param right second element
         * @return negative, zero, or positive as in {@link Comparable#compareTo}
         */
        int compare(Data original, Data current, Data left, Data right);
    }

    /**
     * Creates a sort mapping with a comparator derived from an optional declared function.
     *
     * @param providers argument expression mappings
     * @param function optional comparator function
     */
    public Sort(List<Mapping> providers, List<DeclaredFunction> function) {
        this(providers, function, buildComparator(function));
    }

    private static SortComparator buildComparator(DeclaredFunction fn) {
        return (original, current, left, right) -> {
            var names = fn.parameterNames();
            PathBindings.bind(names.get(0), left);
            PathBindings.bind(names.get(1), right);
            try {
                var frame = fn.closureContext().createChildFrame();
                frame.defineVariable(names.get(0), (o, c) -> left);
                frame.defineVariable(names.get(1), (o, c) -> right);
                var result = fn.accept(original, current, frame);
                if (result.toJson().isInt()) {
                    return result.toJson().asInt();
                } else if (result.toJson().isBoolean()) {
                    return result.toJson().asBoolean() ? 1 : -1;
                } else {
                    throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
                }
            } finally {
                PathBindings.removeBinding(names.get(0));
                PathBindings.removeBinding(names.get(1));
            }
        };
    }

    private static int defaultComparator(Data original, Data current, Data left, Data right) {
        if (left.toJson().isInt()) {
            return Integer.compare(left.toJson().asInt(), right.toJson().asInt());
        } else if (left.toJson().isTextual()) {
            return left.toJson().asText().compareTo(right.toJson().asText());
        } else {
            throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
        }
    }

    private static SortComparator buildComparator(List<DeclaredFunction> fn) {
        return fn.stream()
                 .findFirst()
                 .map(Sort::buildComparator)
                 .orElse(Sort::defaultComparator);
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var sortValue = BuiltInArgs.evaluateOne(providers, original, current);
        if (sortValue.isArray()) {
            return new GroupedData(IntStream.range(0, sortValue.length())
                                            .mapToObj(sortValue::at)
                                            .sorted((left, right) -> comparator.compare(original, current, left, right))
                                            .toList());
        } else {
            return sortValue;
        }
    }

}
