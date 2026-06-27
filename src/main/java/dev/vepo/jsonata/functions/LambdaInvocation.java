package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Immediate invocation of an anonymous function.
 *
 * <p>JSONata construct: {@code function($x){...}(args...)}. Captures current focus as
 * closure context before applying arguments via {@link FunctionApplyService}.
 *
 * @param function     the anonymous function declaration
 * @param argProviders unevaluated call-site arguments
 */
public record LambdaInvocation(DeclaredFunction function, List<Mapping> argProviders) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var fv = function.asValue().withCapturedContext(current);
        return FunctionApplyService.apply(fv, original, current, argProviders);
    }
}
