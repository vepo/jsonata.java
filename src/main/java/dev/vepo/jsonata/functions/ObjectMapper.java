package dev.vepo.jsonata.functions;

import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ObjectMapper(List<FieldContent> contents) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = JsonFactory.objectBuilder();
            contents.forEach(content -> builder.set(content.name().map(original, current).toJson().asText(),
                                                    content.value().map(original, current)));
            return builder.build();
        } else if (current.isArray() || current.isList()) {
            var newContents = new ArrayList<JsonNode>();
            range(0, current.length()).forEach(i -> {
                var builder = JsonFactory.objectBuilder();
                contents.forEach(content -> builder.set(content.name().map(current, current.at(i)).toJson().asText(),
                                                        content.value().map(current, current.at(i)),
                                                        content.merge()));
                newContents.add(builder.root());
            });
            return new ArrayData(JsonFactory.arrayNode(newContents));
        } else {
            return Mapping.empty();
        }
    }
}