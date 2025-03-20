package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

public record CompoundFunction(List<Mapping> functions, CompoundContext context) implements Mapping {

    public static class CompoundContext {
        private Data previousFunctionResult;
        private List<Mapping> inputProviders;

        public Data getPreviousFunctionResult() {
            return previousFunctionResult;
        }
    }

    @Override
    public Data map(Data original, Data current) {
        context.previousFunctionResult = context.inputProviders.get(0).map(original, current);
        for (var fn : functions) {
            context.previousFunctionResult = fn.map(original, current);
        }
        return context.previousFunctionResult;
    }

    public CompoundFunction withProviders(List<Mapping> valueProviders) {
        context.inputProviders = valueProviders;
        return this;
    }

}
