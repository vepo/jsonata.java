package dev.vepo.jsonata.functions;

import java.util.stream.Stream;

/**
 * Boolean operators parsed from JSONata expressions.
 *
 * <p>Used by {@link BooleanExpression} for logical conjunction and disjunction.
 */
public enum BooleanOperator {
    /** Logical and ({@code and}). */
    AND("and"),
    /** Logical or ({@code or}). */
    OR("or");

    /**
     * Parses an operator token from the grammar.
     *
     * @param value the operator text from the parser
     * @return the matching enum constant
     * @throws IllegalStateException when {@code value} is not a known operator
     */
    public static BooleanOperator get(String value) {
        return Stream.of(values())
                     .filter(op -> op.value.compareTo(value) == 0)
                     .findAny()
                     .orElseThrow(() -> new IllegalStateException(String.format("Invalid operator!! operator=%s", value)));
    }

    private String value;

    BooleanOperator(String value) {
        this.value = value;
    }
}
