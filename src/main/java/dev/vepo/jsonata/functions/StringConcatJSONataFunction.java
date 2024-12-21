package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.Function;

import dev.vepo.jsonata.functions.data.Data;

public record StringConcatJSONataFunction(List<Function<Data, Data>> sources) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return stringValue(sources.stream()
                .map(fn -> fn.apply(current).toJson().asText())
                .collect(joining()));
    }
}