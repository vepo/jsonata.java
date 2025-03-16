package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record ObjectBuilderJSONataFunction(List<FieldContent> contents) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = objectBuilder();
            contents.forEach(content -> builder.set(content.name().map(original, current).toJson().asText(),
                                                    content.value().map(original, current)));
            return builder.build();
        } else if (current.isArray() || current.isList()) {
            var builder = objectBuilder(true);
            contents.forEach(content -> {
                if (content.value() instanceof JSONataAggregateFunction aggregate) {
                    toMap(content.name(),
                          aggregate,
                          original,
                          current).forEach((key, value) -> builder.set(key, aggregate.operation(new GroupedData(value))));
                } else {
                    IntStream.range(0, current.length())
                             .forEach(i -> builder.set(content.name().map(original, current.at(i)).toJson().asText(),
                                                       content.value().map(original, current.at(i)),
                                                       content.merge()));
                }
            });
            return builder.build();
        } else {
            return JSONataFunction.empty();
        }
    }

    private Map<String, List<Data>> toMap(JSONataFunction nameExtractor, JSONataAggregateFunction valueExtractor, Data original, Data current) {
        return IntStream.range(0, current.length())
                        .mapToObj(i -> Pair.of(nameExtractor.map(original, current.at(i)).toJson().asText(),
                                               valueExtractor.extractor().map(original, current.at(i))))
                        .collect(Collectors.groupingBy(Pair::getLeft, LinkedHashMap::new, Collectors.mapping(Pair::getRight, Collectors.toList())));
    }

}
