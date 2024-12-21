package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.json2Value;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

public record InnerFunctionJSONataFunction(List<JSONataFunction> inner) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return json2Value(inner.stream().reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                        .map(f -> f.map(original, current)
                                                   .toJson())
                                        .orElse(current.toJson()));
    }
}