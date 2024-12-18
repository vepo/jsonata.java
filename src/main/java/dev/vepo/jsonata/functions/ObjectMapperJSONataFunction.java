package dev.vepo.jsonata.functions;

import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ObjectMapperJSONataFunction(List<FieldContent> contents) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = JsonFactory.objectBuilder();
            contents.forEach(content -> {
                if (content.arrayCast()) {
                    builder.add(content.name().apply(current).toString(),
                                content.value().apply(current));
                } else {
                    builder.set(content.name().apply(current).toString(),
                                content.value().apply(current));
                }
            });
            return builder.build();
        } else if (current.isArray()) {
            var newContents = new ArrayList<JsonNode>();
            range(0, current.length()).forEach(i -> {
                var builder = JsonFactory.objectBuilder();
                contents.forEach(content -> {
                    if (content.arrayCast()) {
                        builder.add(content.name().apply(current.at(i)).toJson().asText(),
                                    content.value().apply(current.at(i)));
                    } else {
                        builder.set(content.name().apply(current.at(i)).toJson().asText(),
                                    content.value().apply(current.at(i)));
                    }
                });
                newContents.add(builder.root());
            });
            return new ArrayData(JsonFactory.arrayNode(newContents));
        } else {
            return JSONataFunction.empty();
        }
    }
}