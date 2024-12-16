package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;
import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.transformers.JsonFactory;
import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.ArrayValue;

public record ObjectMapperExpression(List<FieldContent> contents) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (current.isObject()) {
            var builder = JsonFactory.objectBuilder();
            contents.forEach(
                    content -> builder.set(content.name().apply(current).toString(), content.value().apply(current)));
            return builder.build();
        } else if (current.isArray()) {
            var newContents = new ArrayList<JsonNode>();
            range(0, current.lenght()).forEach(i -> {
                var builder = JsonFactory.objectBuilder();
                contents.forEach(content -> builder.set(content.name().apply(current.at(i)).toJson().asText(),
                                                        content.value().apply(current.at(i))));
                newContents.add(builder.root());
            });
            return new ArrayValue(JsonFactory.arrayNode(newContents));
        } else {
            return empty();
        }
    }
}