package dev.vepo.jsonata.functions;

import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * First-class user-defined function with optional captured context and type signature.
 *
 * <p>Runtime representation of a {@link DeclaredFunction} ready for application.
 * Captured context overrides the current focus when the function was obtained from a
 * variable that closed over a specific value.
 *
 * @param function         the underlying declaration
 * @param capturedContext  optional focus captured at function creation
 * @param signature        optional argument validation signature
 */
public record FunctionValue(DeclaredFunction function, Optional<Data> capturedContext,
                            Optional<FunctionSignature> signature) {

    /**
     * Creates a function value without captured context or signature.
     *
     * @param function the underlying declaration
     */
    public FunctionValue(DeclaredFunction function) {
        this(function, Optional.empty(), Optional.empty());
    }

    /**
     * Returns a copy with the given captured context (e.g. from variable assignment).
     *
     * @param context the focus to use as {@code current} during invocation
     * @return a new function value with captured context set
     */
    public FunctionValue withCapturedContext(Data context) {
        return new FunctionValue(function, Optional.of(context), signature);
    }

    /**
     * Returns a copy with the given argument signature.
     *
     * @param sig the signature to attach
     * @return a new function value with the signature set
     */
    public FunctionValue withSignature(FunctionSignature sig) {
        return new FunctionValue(function, capturedContext, Optional.of(sig));
    }

    /**
     * Invokes this function with evaluated arguments via {@link FunctionApplyService}.
     *
     * @param original     root input document
     * @param current      current focus (overridden by captured context when present)
     * @param argProviders unevaluated call-site arguments
     * @return the invocation result
     */
    public Data invoke(Data original, Data current, List<Mapping> argProviders) {
        return FunctionApplyService.apply(this, original, current, argProviders);
    }

    /**
     * Number of positional parameters declared by the underlying function.
     *
     * @return parameter count (does not include implicit context injection)
     */
    public int arity() {
        return function.parameterNames().size();
    }
}
