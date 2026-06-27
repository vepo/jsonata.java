package dev.vepo.jsonata.functions;

import java.util.stream.Stream;

/**
 * Comparison operators parsed from JSONata expressions.
 *
 * <p>Used by {@link CompareValues} to dispatch equality, ordering, and membership tests.
 */
public enum CompareOperator {
    /** Equality ({@code =}). */
    EQUAL("="),
    /** Inequality ({@code !=}). */
    EQUAL_NOT("!="),
    /** Greater than or equal ({@code >=}). */
    GREATER_THAN(">="),
    /** Strictly greater ({@code >}). */
    GREATER(">"),
    /** Less than or equal ({@code <=}). */
    LESS_THAN("<="),
    /** Strictly less ({@code <}). */
    LESS("<"),
    /** Membership ({@code in}). */
    IN("in");

    /**
     * Parses an operator token from the grammar.
     *
     * @param value the operator text from the parser
     * @return the matching enum constant
     * @throws IllegalStateException when {@code value} is not a known operator
     */
    public static CompareOperator get(String value) {
        return Stream.of(values())
                     .filter(op -> op.value.compareTo(value) == 0)
                     .findAny()
                     .orElseThrow(() -> new IllegalStateException(String.format("Invalid operator!! operator=%s", value)));
    }

    private String value;

    CompareOperator(String value) {
        this.value = value;
    }
}
