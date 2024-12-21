package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;
import static java.util.stream.IntStream.range;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

public record ObjectBuilderJSONataFunction(List<FieldContent> contents) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = objectBuilder();
            contents.forEach(content -> {
                if (content.arrayCast()) {
                    builder.add(content.name().apply(current).toJson().asText(),
                                content.value().apply(current));
                } else {
                    builder.set(content.name().apply(current).toJson().asText(),
                                content.value().apply(current));
                }
            });
            return builder.build();
        } else if (current.isArray()) {
            var builder = objectBuilder(true);
            range(0, current.length()).forEach(i -> contents.forEach(content -> {
                if (content.arrayCast()) {
                    builder.add(content.name().apply(current.at(i)).toJson().asText(),
                                content.value().apply(current.at(i)));
                } else {
                    builder.set(content.name().apply(current.at(i)).toJson().asText(),
                                content.value().apply(current.at(i)));
                }
            }));
            return builder.build();
        } else {
            return JSONataFunction.empty();
        }
    }

}
