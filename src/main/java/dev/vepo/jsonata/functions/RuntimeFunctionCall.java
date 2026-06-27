package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Resolves a function by name from block scope at evaluation time (supports forward references in blocks).
 */
public record RuntimeFunctionCall(BlockContext block, String fnName, List<Mapping> argProviders) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var bound = PathBindings.binding(fnName);
        if (bound.isPresent() && FunctionValues.isFunction(bound.get())) {
            final Data fn = bound.get();
            return FunctionApplyService.apply(FunctionValues.asFunctionValue(fn), original, current, argProviders);
        }
        if (block.compoundFunction(fnName).isPresent()) {
            return block.compoundFunction(fnName).orElseThrow().withProviders(argProviders).map(original, current);
        }
        if (block.function(fnName).isPresent()) {
            return new UserDefinedFunction(argProviders, block.function(fnName).orElseThrow())
                    .map(original, current);
        }
        if (block.variable(fnName).isPresent()) {
            return FunctionApplyService.applyDynamic(block.variable(fnName).orElseThrow(), original, current, argProviders);
        }
        throw new dev.vepo.jsonata.exception.JSONataException("Function not found: " + fnName);
    }
}
