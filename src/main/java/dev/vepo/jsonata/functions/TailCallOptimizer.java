package dev.vepo.jsonata.functions;

import java.util.List;

/**
 * Post-parse pass that wraps tail-position function calls for trampoline execution.
 */
public final class TailCallOptimizer {

    private TailCallOptimizer() {
    }

    public static List<Mapping> optimize(List<Mapping> mappings) {
        return mappings.stream().map(TailCallOptimizer::optimizeMapping).toList();
    }

    private static Mapping optimizeMapping(Mapping mapping) {
        if (mapping instanceof UserDefinedFunction udf) {
            return new UserDefinedFunction(udf.valueProviders(), optimizeDeclared(udf.fn()), udf.capturedContext());
        }
        if (mapping instanceof LambdaInvocation li) {
            return new LambdaInvocation(optimizeDeclared(li.function()), li.argProviders());
        }
        if (mapping instanceof FunctionExpression fe) {
            return new FunctionExpression(optimizeDeclared(fe.function()));
        }
        return mapping;
    }

    public static DeclaredFunction optimizeDeclared(DeclaredFunction fn) {
        var optimizedBody = optimizeBody(fn.body());
        if (optimizedBody == fn.body()) {
            return fn;
        }
        return new DeclaredFunction(fn.parameterNames(), fn.closureContext(), optimizedBody, fn.signature());
    }

    private static Mapping optimizeBody(Mapping body) {
        if (body instanceof TailCallCandidate candidate) {
            return candidate.asTailCall();
        }
        return body;
    }

    /**
     * Function calls that may be converted to tail-call thunks.
     */
    public interface TailCallCandidate extends Mapping {
        Mapping asTailCall();
    }
}
