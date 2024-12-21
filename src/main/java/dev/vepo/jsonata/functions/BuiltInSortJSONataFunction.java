package dev.vepo.jsonata.functions;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record BuiltInSortJSONataFunction(FieldPathJSONataFunction extractor, Optional<DeclaredFunction> function,
                                         Comparator<Data> comparator)
        implements JSONataFunction {
    public BuiltInSortJSONataFunction(FieldPathJSONataFunction extractor, Optional<DeclaredFunction> function) {
        this(extractor, function, buildComparator(function));
    }

    private static Comparator<Data> buildComparator(Optional<DeclaredFunction> fn) {
        if (fn.isPresent()) {
            return (left, right) -> {
                var result = fn.get()
                               .accept(left, right);
                if (result.toJson().isInt()) {
                    return result.toJson().asInt();
                } else if (result.toJson().isBoolean()) {
                    return result.toJson().asBoolean() ? 1 : -1;
                } else {
                    throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
                }
            };
        } else {
            return (left, right) -> {
                if (left.toJson().isInt()) {
                    return Integer.compare(left.toJson().asInt(), right.toJson().asInt());
                } else if (left.toJson().isTextual()) {
                    return left.toJson().asText().compareTo(right.toJson().asText());
                } else {
                    throw new JSONataException(String.format("Cannot compare values!!! left=%s right=%s", left, right));
                }
            };
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var sortValue = extractor.map(original, current);
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
