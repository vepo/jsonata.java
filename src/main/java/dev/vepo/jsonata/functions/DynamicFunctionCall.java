package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Function call through a dynamic target (variable or expression).
 *
 * <p>JSONata construct: {@code $fn(args...)} where {@code $fn} resolves to a function
 * value at runtime. Delegates to {@link FunctionApplyService#applyDynamic}.
 *
 * @param targetResolver expression yielding the function to invoke
 * @param argProviders   unevaluated call-site arguments
 */
public record DynamicFunctionCall(Mapping targetResolver, List<Mapping> argProviders) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return FunctionApplyService.applyDynamic(targetResolver, original, current, argProviders);
    }
}
