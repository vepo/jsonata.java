package dev.vepo.jsonata;

import java.util.List;

/**
 * Caller-facing view of a JSONata evaluation outcome.
 * <p>
 * Layer: <strong>application</strong> (public result contract). Produced by {@link JSONata#evaluate}
 * and {@link JSONata#evaluateData}; backed by domain implementations in {@code results}.
 * <p>
 * Scalar accessors coerce the primary value; {@link #multi()} exposes the JSONata sequence
 * (zero or more values). Use {@link #isEmpty()} for the spec empty sequence and {@link #isNull()}
 * for JSON {@code null}; they are distinct.
 * <p>
 * Invariants: instances are views over evaluation output; thread-safety follows the backing
 * implementation (typically immutable after evaluation completes).
 */
public interface JSONataResult {

    /**
     * Primary value as a JSON text or serialized object representation.
     *
     * @return textual form suitable for display or logging
     * @throws IllegalStateException if this result is {@linkplain #isEmpty() empty}
     */
    String asText();

    /**
     * Primary value coerced to {@code int} (Jackson numeric rules).
     *
     * @return integer value
     * @throws IllegalStateException if this result is {@linkplain #isEmpty() empty}
     */
    int asInt();

    /**
     * Whether the primary value is an integer numeric type.
     *
     * @return {@code true} when the value is an integral number
     */
    boolean isInt();

    /**
     * Primary value coerced to {@code double} (Jackson numeric rules).
     *
     * @return floating-point value
     * @throws IllegalStateException if this result is {@linkplain #isEmpty() empty}
     */
    double asDouble();

    /**
     * Whether the primary value is a floating-point numeric type.
     *
     * @return {@code true} when the value is a double-precision number
     */
    boolean isDouble();

    /**
     * Primary value coerced to {@code boolean} (Jackson boolean rules).
     *
     * @return boolean value
     * @throws IllegalStateException if this result is {@linkplain #isEmpty() empty}
     */
    boolean asBoolean();

    /**
     * Whether the primary value is JSON {@code null}.
     *
     * @return {@code true} for JSON null; {@code false} for empty sequences and other types
     */
    boolean isNull();

    /**
     * Whether this result represents the JSONata empty sequence (no value).
     *
     * @return {@code true} when no value is present; scalar accessors throw in this state
     */
    boolean isEmpty();

    /**
     * Sequence view of all values produced by the expression.
     * <p>
     * A singleton result yields a one-element list; an empty result yields empty lists without
     * throwing.
     *
     * @return multi-value accessor for this result
     */
    Multi multi();

    /**
     * Sequence of values from a JSONata expression result.
     * <p>
     * Used when an expression may produce zero, one, or many values (e.g. path wildcards,
     * predicates). List accessors coerce each element independently.
     */
    public interface Multi {

        /**
         * Each sequence element as text.
         *
         * @return one entry per value in the sequence; empty when the result is empty
         */
        List<String> asText();

        /**
         * Each sequence element coerced to {@code int}.
         *
         * @return one entry per value in the sequence; empty when the result is empty
         */
        List<Integer> asInt();

        /**
         * Each sequence element coerced to {@code boolean}.
         *
         * @return one entry per value in the sequence; empty when the result is empty
         */
        List<Boolean> asBoolean();

        /**
         * Each sequence element coerced to {@code double}.
         *
         * @return one entry per value in the sequence; empty when the result is empty
         */
        List<Double> asDouble();
    }
}
