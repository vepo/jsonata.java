package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.TailCallData;

/**
 * Function call in tail position, deferred for trampoline execution.
 *
 * <p>Instead of invoking immediately, {@link #map} returns {@link TailCallData} so
 * {@link FunctionApplyService} can loop without growing the Java call stack.
 *
 * @param delegate     the call target (function value or dynamic resolver)
 * @param argProviders unevaluated call-site arguments
 */
public record TailCallFunctionCall(Mapping delegate, List<Mapping> argProviders) implements Mapping, TailCallOptimizer.TailCallCandidate {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return new TailCallData(new TailCallThunk(delegate, List.copyOf(argProviders), original, current));
    }

    /** {@inheritDoc} */
    @Override
    public Mapping asTailCall() {
        return this;
    }
}
