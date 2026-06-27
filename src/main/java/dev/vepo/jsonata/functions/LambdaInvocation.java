package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Immediate invocation of an anonymous function: {@code function($x){...}(args)}.
 */
public record LambdaInvocation(DeclaredFunction function, List<Mapping> argProviders) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var fv = function.asValue().withCapturedContext(current);
        return FunctionApplyService.apply(fv, original, current, argProviders);
    }
}
