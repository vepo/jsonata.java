package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

public record UserDefinedFunctionJSONataFunction(List<JSONataFunction> valueProviders, DeclaredFunction fn) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return fn.accept(valueProviders.stream()
                                       .map(provider -> provider.map(original, current))
                                       .toArray(Data[]::new));
    }

}
