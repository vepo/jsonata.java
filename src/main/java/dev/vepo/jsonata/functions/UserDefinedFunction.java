package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Named user-defined function call resolved from block scope.
 */
public record UserDefinedFunction(List<Mapping> valueProviders, DeclaredFunction fn,
                                  Data capturedContext) implements Mapping {

    public UserDefinedFunction(List<Mapping> valueProviders, DeclaredFunction fn) {
        this(valueProviders, fn, null);
    }

    @Override
    public Data map(Data original, Data current) {
        var fv = capturedContext != null
                ? fn.asValue().withCapturedContext(capturedContext)
                : fn.asValue();
        return FunctionApplyService.apply(fv, original, current, valueProviders);
    }
}
