package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.TailCallData;

/**
 * Wraps a function call so the apply pipeline can trampoline it.
 */
public record TailCallFunctionCall(Mapping delegate, List<Mapping> argProviders) implements Mapping, TailCallOptimizer.TailCallCandidate {

    @Override
    public Data map(Data original, Data current) {
        return new TailCallData(new TailCallThunk(delegate, List.copyOf(argProviders), original, current));
    }

    @Override
    public Mapping asTailCall() {
        return this;
    }
}
