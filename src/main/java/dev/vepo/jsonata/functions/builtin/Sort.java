package dev.vepo.jsonata.functions.builtin;

import java.util.List;
import java.util.stream.IntStream;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record Sort(List<Mapping> providers, List<DeclaredFunction> function,
                   SortComparator comparator)
        implements Mapping {
    public Sort {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$sort function must have 1 argument");
        }
    }

    @FunctionalInterface
    public interface SortComparator {
        int compare(Data original, Data current, Data left, Data right);
    }

    public Sort(List<Mapping> providers, List<DeclaredFunction> function) {
        this(providers, function, buildComparator(function));
    }

    private static SortComparator buildComparator(DeclaredFunction fn) {
        return (original, current, left, right) -> {
            fn.context().defineVariable(fn.parameterNames().get(0), (o, c) -> left);
            fn.context().defineVariable(fn.parameterNames().get(1), (o, c) -> right);
            var result = fn.accept(original, current, fn.context());
            if (result.toJson().isInt()) {
                return result.toJson().asInt();
            } else if (result.toJson().isBoolean()) {
                return result.toJson().asBoolean() ? 1 : -1;
            } else {
                throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
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

    @Override
    public Data map(Data original, Data current) {
        var sortValue = providers.get(0).map(original, current);
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
