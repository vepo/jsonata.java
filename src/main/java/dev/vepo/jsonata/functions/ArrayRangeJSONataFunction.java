package dev.vepo.jsonata.functions;

import static java.lang.Math.min;
import static java.util.stream.IntStream.range;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

public record ArrayRangeJSONataFunction(int start, int end) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (!current.isArray() && start == 0) {
            return current;
        }
        if (current.isArray() && start < current.length()) {
            return new GroupedData(range(start, min(end + 1, current.length())).mapToObj(current::at)
                                                                                .toList());
        } else {
            return JSONataFunction.empty();
        }
    }

}