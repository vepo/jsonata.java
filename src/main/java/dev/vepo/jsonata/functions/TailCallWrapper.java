package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Post-parse wrapper for a function body whose last expression is a bare tail call.
 *
 * <p>Implements {@link TailCallMapping} so {@link DeclaredFunction#accept} can invoke
 * the body with the correct invocation frame when needed.
 *
 * @param inner the optimized tail-call body mapping
 */
public record TailCallWrapper(Mapping inner) implements TailCallMapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return inner.map(original, current);
    }

    /**
     * Evaluates the wrapped body with the function invocation frame.
     *
     * @param original root input document
     * @param current  effective context after variable overlay
     * @param frame    the invocation frame for parameter bindings
     * @return the body result
     */
    @Override
    public Data mapWithFrame(Data original, Data current, BlockContext frame) {
        return inner.map(original, current);
    }
}
