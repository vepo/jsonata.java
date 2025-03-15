package dev.vepo.jsonata.functions;

import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;

public record InlineIfJSONataFunction(JSONataFunction testProvider, JSONataFunction trueValueProvider, Optional<JSONataFunction> falseValueProvider)
        implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var testValue = testProvider.map(original, current).toJson();
        if (testValue.isBoolean() && testValue.asBoolean()) {
            return trueValueProvider.map(original, current);
        } else {
            return falseValueProvider.map(fn -> fn.map(original, current))
                                     .orElseGet(JSONataFunction::empty);
        }
    }

}
