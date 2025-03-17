package dev.vepo.jsonata.functions;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record BuiltInSortJSONataFunction(List<JSONataFunction> providers, Optional<DeclaredFunction> function,
                                         Comparator<Data> comparator)
        implements JSONataFunction {
    public BuiltInSortJSONataFunction {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("Sort function must have 1 argument");
        }
    }

    public BuiltInSortJSONataFunction(List<JSONataFunction> providers, Optional<DeclaredFunction> function) {
        this(providers, function, buildComparator(function));
    }

    private static Comparator<Data> buildComparator(DeclaredFunction fn) {
        return (left, right) -> {
            var result = fn.accept(left, right);
            if (result.toJson().isInt()) {
                return result.toJson().asInt();
            } else if (result.toJson().isBoolean()) {
                return result.toJson().asBoolean() ? 1 : -1;
            } else {
                throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
            }
        };
    }

    private static int defaultComparator(Data left, Data right) {
        if (left.toJson().isInt()) {
            return Integer.compare(left.toJson().asInt(), right.toJson().asInt());
        } else if (left.toJson().isTextual()) {
            return left.toJson().asText().compareTo(right.toJson().asText());
        } else {
            throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
        }
    }

    private static Comparator<Data> buildComparator(Optional<DeclaredFunction> fn) {
        return fn.map(BuiltInSortJSONataFunction::buildComparator)
                 .orElse(BuiltInSortJSONataFunction::defaultComparator);
    }

    @Override
    public Data map(Data original, Data current) {
        var sortValue = providers.get(0).map(original, current);
        if (sortValue.isArray()) {
            return new GroupedData(IntStream.range(0, sortValue.length())
                                            .mapToObj(sortValue::at)
                                            .sorted(comparator)
                                            .toList());
        } else {
            return sortValue;
        }
    }

}
