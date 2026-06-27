package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Named user-defined function call resolved from block scope.
 *
 * <p>Represents {@code $name(args...)} where {@code $name} was declared in a block.
 * Optionally carries captured context when the function value came from a variable
 * assignment that closed over the current focus.
 *
 * @param valueProviders unevaluated call-site arguments
 * @param fn             the declared function from block scope
 * @param capturedContext optional captured focus from a variable binding, or {@code null}
 */
public record UserDefinedFunction(List<Mapping> valueProviders, DeclaredFunction fn,
                                  Data capturedContext) implements Mapping {

    /**
     * Creates a call without captured context.
     *
     * @param valueProviders unevaluated call-site arguments
     * @param fn             the declared function
     */
    public UserDefinedFunction(List<Mapping> valueProviders, DeclaredFunction fn) {
        this(valueProviders, fn, null);
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var fv = capturedContext != null
                ? fn.asValue().withCapturedContext(capturedContext)
                : fn.asValue();
        return FunctionApplyService.apply(fv, original, current, valueProviders);
    }
}
