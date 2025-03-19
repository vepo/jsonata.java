package dev.vepo.jsonata.functions;

import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;

public record InlineIf(Mapping testProvider, Mapping trueValueProvider, Optional<Mapping> falseValueProvider)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var testValue = testProvider.map(original, current).toJson();
        if (testValue.isBoolean() && testValue.asBoolean()) {
            return trueValueProvider.map(original, current);
        } else {
            return falseValueProvider.map(fn -> fn.map(original, current))
                                     .orElseGet(Mapping::empty);
        }
    }

}
