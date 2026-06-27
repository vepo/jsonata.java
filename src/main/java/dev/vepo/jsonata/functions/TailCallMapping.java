package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Function body mapping that supports tail-call evaluation with an invocation frame.
 *
 * <p>Extends {@link Mapping} with {@link #mapWithFrame} so {@link DeclaredFunction#accept}
 * can pass the parameter overlay context when executing optimized tail-call bodies.
 */
public interface TailCallMapping extends Mapping {

    /**
     * Evaluates this mapping with the function invocation frame available.
     *
     * @param original root input document
     * @param current  effective context after variable overlay
     * @param frame    the invocation frame holding parameter bindings
     * @return the body result
     */
    Data mapWithFrame(Data original, Data current, BlockContext frame);
}
