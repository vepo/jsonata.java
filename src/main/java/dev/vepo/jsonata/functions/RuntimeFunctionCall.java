package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Resolves a function by name from block scope at evaluation time.
 *
 * <p>Deferred resolution supports forward references in blocks: a call may appear
 * before its {@code $name := function(...)} assignment. Resolution order: path binding,
 * compound built-in override, declared function, then variable holding a function value.
 *
 * @param block        the block scope for name lookup
 * @param fnName       the function name
 * @param argProviders unevaluated call-site arguments
 */
public record RuntimeFunctionCall(BlockContext block, String fnName, List<Mapping> argProviders) implements Mapping {

    /** {@inheritDoc} */
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
