package dev.vepo.jsonata.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.builtin.BuiltInSupplier;
import dev.vepo.jsonata.functions.builtin.ContainsJSONataFunction;
import dev.vepo.jsonata.functions.builtin.LengthJSONataFunction;
import dev.vepo.jsonata.functions.builtin.LowecaseJSONataFunction;
import dev.vepo.jsonata.functions.builtin.PadJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SortJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SplitJSONataFunction;
import dev.vepo.jsonata.functions.builtin.StringJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SubstringAfterJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SubstringBeforeJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SubstringJSONataFunction;
import dev.vepo.jsonata.functions.builtin.SumJSONataFunction;
import dev.vepo.jsonata.functions.builtin.TrimJSONataFunction;
import dev.vepo.jsonata.functions.builtin.UppercaseJSONataFunction;

public enum BuiltInFunction {
    SORT("$sort", SortJSONataFunction::new),
    SUM("$sum", SumJSONataFunction::new),
    STRING("$string", StringJSONataFunction::new),
    LENGTH("$length", LengthJSONataFunction::new),
    SUBSTRING("$substring", SubstringJSONataFunction::new),
    SUBSTRING_BEFORE("$substringBefore", SubstringBeforeJSONataFunction::new),
    SUBSTRING_AFTER("$substringAfter", SubstringAfterJSONataFunction::new),
    LOWERCASE("$lowercase", LowecaseJSONataFunction::new),
    UPPERCASE("$uppercase", UppercaseJSONataFunction::new),
    TRIM("$trim", TrimJSONataFunction::new),
    PAD("$pad", PadJSONataFunction::new),
    CONTAINS("$contains", ContainsJSONataFunction::new),
    SPLIT("$split", SplitJSONataFunction::new);

    public static Optional<BuiltInFunction> get(String name) {
        return Stream.of(values())
                     .filter(n -> n.name.compareToIgnoreCase(name) == 0)
                     .findAny();
    }

    private final String name;
    private final BuiltInSupplier supplier;

    BuiltInFunction(String name, BuiltInSupplier supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    JSONataFunction instantiate(List<JSONataFunction> valueProviders, List<DeclaredFunction> functions) {
        return supplier.instantiate(valueProviders, functions);
    }
}