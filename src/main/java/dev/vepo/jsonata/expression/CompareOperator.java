package dev.vepo.jsonata.expression;

import java.util.stream.Stream;

public enum CompareOperator {
    EQUAL("="),
    EQUAL_NOT("!="),
    GREATER_THAN(">="),
    GREATER(">"),
    LESS_THAN("<="),
    LESS("<"),
    IN("in");

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