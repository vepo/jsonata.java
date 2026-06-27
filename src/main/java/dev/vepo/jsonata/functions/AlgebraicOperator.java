package dev.vepo.jsonata.functions;

import java.util.stream.Stream;

/**
 * Arithmetic operators parsed from JSONata expressions.
 *
 * <p>Used by {@link AlgebraicOperation} to dispatch numeric computation and array broadcasting.
 */
public enum AlgebraicOperator {
    /** Addition ({@code +}). */
    ADD("+"),
    /** Subtraction ({@code -}). */
    SUBTRACT("-"),
    /** Multiplication ({@code *}). */
    MULTIPLY("*"),
    /** Division ({@code /}). */
    DIVIDE("/"),
    /** Modulo ({@code %}). */
    MODULO("%"),
    /** Exponentiation ({@code ^}). */
    POWER("^");

    /**
     * Parses an operator token from the grammar.
     *
     * @param value the operator text from the parser
     * @return the matching enum constant
     * @throws IllegalStateException when {@code value} is not a known operator
     */
    public static AlgebraicOperator get(String value) {
        return Stream.of(values())
                     .filter(op -> op.value.compareTo(value) == 0)
                     .findAny()
                     .orElseThrow(() -> new IllegalStateException(String.format("Invalid operator!! operator=%s", value)));
    }

    private String value;

    AlgebraicOperator(String value) {
        this.value = value;
    }
}
