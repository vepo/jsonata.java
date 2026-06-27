package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.EmptyData;

/**
 * Core evaluation abstraction for compiled JSONata expression fragments.
 *
 * <p>Every syntactic construct that produces a value at runtime implements {@code Mapping}.
 * Evaluation is purely functional at this layer: {@link #map(Data, Data)} receives two
 * contexts and returns a {@link Data} result without mutating either input.
 *
 * <h2>Evaluation contexts</h2>
 * <ul>
 *   <li>{@code original} — the root input document, passed unchanged through the entire
 *       evaluation tree so expressions can refer back to the full payload (e.g. block
 *       variables, parent references).</li>
 *   <li>{@code current} — the focus value for this step: the result of prior path
 *       navigation, block variable overlay, or parent binding. Most constructs read and
 *       transform {@code current}; {@code original} is consulted when the expression
 *       explicitly needs the root.</li>
 * </ul>
 *
 * <p>A result of {@link #empty()} ({@link EmptyData}) represents JSONata's effective
 * absence of value at this position — callers treat it as empty/undefined rather than
 * {@code null}.
 *
 * @see ChainedMapping
 * @see MappingJoin
 * @see BlockSequence
 */
@FunctionalInterface
public interface Mapping {

    /**
     * Evaluates this mapping against the given evaluation contexts.
     *
     * @param original the root input document, unchanged throughout evaluation
     * @param current  the focus value for this step (path result, block overlay, etc.)
     * @return the mapped result; {@link EmptyData} when the construct yields no value
     */
    Data map(Data original, Data current);

    /**
     * Returns the canonical empty result used when a JSONata construct produces no value.
     *
     * @return a shared {@link EmptyData} instance
     */
    public static Data empty() {
        return new EmptyData();
    }

    /**
     * Composes this mapping with {@code after}: the output of {@code this} becomes the
     * {@code current} focus passed to {@code after}, while {@code original} is forwarded
     * unchanged. Equivalent to sequential path steps ({@code a.b}).
     *
     * @param after the mapping to run after this one
     * @return a {@link ChainedMapping} that evaluates {@code this} then {@code after}
     */
    default Mapping andThen(Mapping after) {
        return new ChainedMapping(this, after);
    }
}
