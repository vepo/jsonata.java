package dev.vepo.jsonata.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.builtin.Average;
import dev.vepo.jsonata.functions.builtin.BuiltInSupplier;
import dev.vepo.jsonata.functions.builtin.Contains;
import dev.vepo.jsonata.functions.builtin.Count;
import dev.vepo.jsonata.functions.builtin.FnString;
import dev.vepo.jsonata.functions.builtin.Join;
import dev.vepo.jsonata.functions.builtin.Length;
import dev.vepo.jsonata.functions.builtin.Lowecase;
import dev.vepo.jsonata.functions.builtin.Max;
import dev.vepo.jsonata.functions.builtin.Min;
import dev.vepo.jsonata.functions.builtin.Pad;
import dev.vepo.jsonata.functions.builtin.Sort;
import dev.vepo.jsonata.functions.builtin.Split;
import dev.vepo.jsonata.functions.builtin.Substring;
import dev.vepo.jsonata.functions.builtin.SubstringAfter;
import dev.vepo.jsonata.functions.builtin.SubstringBefore;
import dev.vepo.jsonata.functions.builtin.Sum;
import dev.vepo.jsonata.functions.builtin.Trim;
import dev.vepo.jsonata.functions.builtin.Uppercase;

public enum BuiltInFunction {
    SORT("$sort", Sort::new),
    SUM("$sum", Sum::new),
    STRING("$string", FnString::new),
    LENGTH("$length", Length::new),
    SUBSTRING("$substring", Substring::new),
    SUBSTRING_BEFORE("$substringBefore", SubstringBefore::new),
    SUBSTRING_AFTER("$substringAfter", SubstringAfter::new),
    LOWERCASE("$lowercase", Lowecase::new),
    UPPERCASE("$uppercase", Uppercase::new),
    TRIM("$trim", Trim::new),
    PAD("$pad", Pad::new),
    CONTAINS("$contains", Contains::new),
    SPLIT("$split", Split::new),
    MAX("$max", Max::new),
    MIN("$min", Min::new),
    AVERAGE("$average", Average::new),
    COUNT("$count", Count::new),
    JOIN("$join", Join::new);

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

    Mapping instantiate(List<Mapping> valueProviders, List<DeclaredFunction> functions) {
        return supplier.instantiate(valueProviders, functions);
    }
}