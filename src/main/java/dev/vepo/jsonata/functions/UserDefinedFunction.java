package dev.vepo.jsonata.functions;

import java.util.List;
import java.util.stream.IntStream;

import dev.vepo.jsonata.functions.data.Data;

public record UserDefinedFunction(List<Mapping> valueProviders, DeclaredFunction fn) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        IntStream.range(0, fn.parameterNames().size())
                 .forEach(i -> fn.context()
                                 .defineVariable(fn.parameterNames().get(i), valueProviders.get(i)));
        return fn.accept(original, current, fn.context());
    }

}
