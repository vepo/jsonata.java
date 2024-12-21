package dev.vepo.jsonata.functions;

import java.util.List;
import java.util.stream.IntStream;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record DeclaredFunction(List<String> parameterNames, List<JSONataFunction> functions) {
    Data accept(Data... args) {
        var input = JsonFactory.objectBuilder();
        IntStream.range(0, parameterNames.size())
                 .forEach(i -> input.set(parameterNames.get(i), args[i]));
        var inputData = input.build();

        return functions.stream()
                        .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                        .map(f -> f.map(inputData, inputData))
                        .orElse(inputData);
    }
}
