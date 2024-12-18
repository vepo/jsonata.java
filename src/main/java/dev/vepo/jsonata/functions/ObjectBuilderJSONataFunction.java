package dev.vepo.jsonata.functions;

import static java.util.stream.IntStream.range;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ObjectBuilderJSONataFunction(List<FieldContent> contents) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = JsonFactory.objectBuilder();
            contents.forEach(
                             content -> builder.set(content.name().apply(current).toString(), content.value().apply(current)));
            return builder.build();
        } else if (current.isArray()) {
            var builder = JsonFactory.objectBuilder(true);
            range(0, current.length()).forEach(i -> {
                contents.forEach(content -> {
                    builder.set(content.name().apply(current.at(i)).toJson().asText(),
                                content.value().apply(current.at(i)));
                });
            });
            return builder.build();
        } else {
            return JSONataFunction.empty();
        }
    }

}
