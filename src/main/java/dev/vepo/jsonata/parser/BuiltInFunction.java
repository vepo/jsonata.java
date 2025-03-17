package dev.vepo.jsonata.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.buildin.BuiltInSupplier;
import dev.vepo.jsonata.functions.buildin.ContainsJSONataFunction;
import dev.vepo.jsonata.functions.buildin.LengthJSONataFunction;
import dev.vepo.jsonata.functions.buildin.LowecaseJSONataFunction;
import dev.vepo.jsonata.functions.buildin.PadJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SortJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SplitJSONataFunction;
import dev.vepo.jsonata.functions.buildin.StringJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SubstringAfterJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SubstringBeforeJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SubstringJSONataFunction;
import dev.vepo.jsonata.functions.buildin.SumJSONataFunction;
import dev.vepo.jsonata.functions.buildin.TrimJSONataFunction;
import dev.vepo.jsonata.functions.buildin.UppercaseJSONataFunction;

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