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
                builder.set(content.name().map(original, current).toJson().asText(),
                            content.value().map(original, current));
            });
            return builder.build();
        } else if (current.isArray()) {
            var builder = objectBuilder(true);
            range(0, current.length()).forEach(i -> contents.forEach(content -> {
                builder.set(content.name().map(original, current.at(i)).toJson().asText(),
                            content.value().map(original, current.at(i)));
            }));
            return builder.build();
        } else {
            return JSONataFunction.empty();
        }
    }

}
