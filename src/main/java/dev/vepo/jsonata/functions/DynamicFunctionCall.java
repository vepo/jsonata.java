package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Resolves a variable to a function value and applies arguments.
 */
public record DynamicFunctionCall(Mapping targetResolver, List<Mapping> argProviders) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return FunctionApplyService.applyDynamic(targetResolver, original, current, argProviders);
    }
}
