package dev.vepo.jsonata.functions;

import java.util.List;

/**
 * Post-parse pass that wraps tail-position function calls for trampoline execution.
 *
 * <p>When a user-defined function body's last expression is a bare recursive call,
 * converting it to {@link TailCallFunctionCall} allows {@link FunctionApplyService} to
 * loop via {@link dev.vepo.jsonata.functions.data.TailCallData} instead of growing the
 * Java stack.
 */
public final class TailCallOptimizer {

    private TailCallOptimizer() {
    }

    /**
     * Optimizes all top-level mappings from a parse result.
     *
     * @param mappings compiled mappings from the parser listener
     * @return mappings with tail-call candidates wrapped where applicable
     */
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

    /**
     * Optimizes a declared function's body for tail-call execution.
     *
     * @param fn the function to optimize
     * @return the same instance if unchanged, or a copy with an optimized body
     */
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

        /**
         * Returns the tail-call optimized form of this call.
         *
         * @return a {@link TailCallFunctionCall} or equivalent
         */
        Mapping asTailCall();
    }
}
