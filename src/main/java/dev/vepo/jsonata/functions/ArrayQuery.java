package dev.vepo.jsonata.functions;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record ArrayQuery(Mapping mapFunction, Mapping filterFunction) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var mapped = mapFunction.map(original, current);
        if (filterFunction instanceof ArrayExpansion || filterFunction instanceof ArrayConstructor) {
            List<Integer> indexes = filterFunction.map(original, current)
                                                  .stream()
                                                  .map(Data::toJson)
                                                  .map(node -> node.asInt())
                                                  .collect(Collectors.toList());
            if (!(mapped.isArray() || mapped.isList()) && indexes.contains(0)) {
                return mapped;
            }
            return new GroupedData(indexes.stream()
                                          .map(i -> i >= 0 ? i : (mapped.length() + i))
                                          .filter(i -> i < mapped.length())
                                          .sorted()
                                          .map(mapped::at)
                                          .toList());
        } else if (mapped.isArray() || mapped.isList()) {
            return new GroupedData(IntStream.range(0, mapped.length())
                                            .mapToObj(mapped::at)
                                            .filter(currData -> {
                                                var currResult = filterFunction.map(original, currData).toJson();
                                                return currResult.isBoolean() && currResult.asBoolean();
                                            }).toList());
        } else {
            return Mapping.empty();
        }
    }

}
