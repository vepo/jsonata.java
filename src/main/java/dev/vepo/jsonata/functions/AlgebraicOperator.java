package dev.vepo.jsonata.functions;

import java.util.stream.Stream;

public enum AlgebraicOperator {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    POWER("^");

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
