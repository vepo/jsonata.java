package dev.vepo.jsonata.functions;

import java.util.function.Function;

import dev.vepo.jsonata.functions.data.Data;

public record FieldContent(Function<Data, Data> name, Function<Data, Data> value) {
}