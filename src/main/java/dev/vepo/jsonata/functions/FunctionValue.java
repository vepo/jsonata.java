package dev.vepo.jsonata.functions;

import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * First-class user-defined function with optional captured context and signature.
 */
public record FunctionValue(DeclaredFunction function, Optional<Data> capturedContext,
                            Optional<FunctionSignature> signature) {

    public FunctionValue(DeclaredFunction function) {
        this(function, Optional.empty(), Optional.empty());
    }

    public FunctionValue withCapturedContext(Data context) {
        return new FunctionValue(function, Optional.of(context), signature);
    }

    public FunctionValue withSignature(FunctionSignature sig) {
        return new FunctionValue(function, capturedContext, Optional.of(sig));
    }

    public Data invoke(Data original, Data current, List<Mapping> argProviders) {
        return FunctionApplyService.apply(this, original, current, argProviders);
    }

    public int arity() {
        return function.parameterNames().size();
    }
}
