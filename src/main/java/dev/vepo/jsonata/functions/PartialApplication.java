package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * Function with some arguments fixed and {@code ?} placeholders for the rest.
 *
 * <p>JSONata partial application: {@code $sum(?)} or {@code function($a,$b){...}(?, 2)}.
 * {@link #PLACEHOLDER} marks unfilled slots; {@link FunctionApplyService#applyPartial}
 * merges subsequent call-site arguments.
 *
 * @param target    the underlying function (user, built-in wrapper, or registered wrapper)
 * @param boundArgs evaluated values and {@link #PLACEHOLDER} markers in parameter order
 * @param signature optional type signature when known
 */
public record PartialApplication(Object target, List<Object> boundArgs, Optional<FunctionSignature> signature) {

    /** Sentinel marking an argument slot awaiting a call-site value. */
    public static final Object PLACEHOLDER = new Object();

    /**
     * Creates a partial application with all argument slots unfilled.
     *
     * @param target the function to partially apply
     * @param arity  total number of parameters
     */
    public PartialApplication(Object target, int arity) {
        this(target, new ArrayList<>(java.util.Collections.nCopies(arity, PLACEHOLDER)), Optional.empty());
    }

    /**
     * Returns whether all placeholder slots have been filled.
     *
     * @return {@code true} when no {@link #PLACEHOLDER} entries remain
     */
    public boolean isComplete() {
        return boundArgs.stream().noneMatch(PartialApplication.PLACEHOLDER::equals);
    }

    /**
     * Counts remaining unfilled placeholder slots.
     *
     * @return number of {@link #PLACEHOLDER} entries in {@link #boundArgs}
     */
    public int remainingPlaceholders() {
        return (int) boundArgs.stream().filter(PartialApplication.PLACEHOLDER::equals).count();
    }

    /**
     * Applies call-site arguments to this partial application.
     *
     * @param original     root input document
     * @param current      current focus
     * @param argProviders arguments for remaining placeholders
     * @return the result or a further partial wrapped in {@link dev.vepo.jsonata.functions.data.FunctionData}
     */
    public Data invoke(Data original, Data current, List<Mapping> argProviders) {
        return FunctionApplyService.applyPartial(this, original, current, argProviders);
    }
}
