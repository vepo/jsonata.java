package dev.vepo.jsonata.parser;

import java.util.Optional;
import java.util.stream.Stream;

public enum BuiltInFunction {
    SORT("$sort"),
    SUM("$sum"),
    STRING("$string"),
    LENGTH("$length"),
    SUBSTRING("$substring"),
    SUBSTRING_BEFORE("$substringBefore"),
    SUBSTRING_AFTER("$substringAfter"),
    LOWERCASE("$lowercase"),
    UPPERCASE("$uppercase");

    public static Optional<BuiltInFunction> get(String name) {
        return Stream.of(values())
                     .filter(n -> n.name.compareToIgnoreCase(name) == 0)
                     .findAny();
    }

    private String name;

    BuiltInFunction(String name) {
        this.name = name;
    }
}