package dev.vepo.jsonata.functions;

import java.util.stream.Stream;

public enum BooleanOperator {
    AND("and"),
    OR("or");

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