package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * Function with some arguments fixed and {@code ?} placeholders for the rest.
 */
public record PartialApplication(Object target, List<Object> boundArgs, Optional<FunctionSignature> signature) {

    public static final Object PLACEHOLDER = new Object();

    public PartialApplication(Object target, int arity) {
        this(target, new ArrayList<>(java.util.Collections.nCopies(arity, PLACEHOLDER)), Optional.empty());
    }

    public boolean isComplete() {
        return boundArgs.stream().noneMatch(PartialApplication.PLACEHOLDER::equals);
    }

    public int remainingPlaceholders() {
        return (int) boundArgs.stream().filter(PartialApplication.PLACEHOLDER::equals).count();
    }

    public Data invoke(Data original, Data current, List<Mapping> argProviders) {
        return FunctionApplyService.applyPartial(this, original, current, argProviders);
    }
}
