package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

@FunctionalInterface
public interface JSONataFunction {
    Data map(Data original, Data current);
}
