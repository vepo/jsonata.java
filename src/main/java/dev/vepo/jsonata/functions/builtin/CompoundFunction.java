package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata function composition: evaluates a sequence of built-ins, feeding each
 * result as context to the next.
 */
public record CompoundFunction(List<Mapping> functions, CompoundContext context) implements Mapping {

    /**
     * Mutable evaluation state shared across a composed function chain.
     */
    public static class CompoundContext {
        private Data previousFunctionResult;
        private List<Mapping> inputProviders;

        /**
         * Result produced by the most recently evaluated step in the chain.
         *
         * @return previous step result, or {@code null} before the first step
         */
        public Data getPreviousFunctionResult() {
            return previousFunctionResult;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        context.previousFunctionResult = context.inputProviders.get(0).map(original, current);
        for (var fn : functions) {
            context.previousFunctionResult = fn.map(original, current);
        }
        return context.previousFunctionResult;
    }

    /**
     * Binds the input providers whose first value seeds the composition chain.
     *
     * @param valueProviders call-site argument mappings
     * @return this composition with providers attached
     */
    public CompoundFunction withProviders(List<Mapping> valueProviders) {
        context.inputProviders = valueProviders;
        return this;
    }

}
