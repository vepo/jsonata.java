package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataFunction;

@FunctionalInterface
public interface BuiltInSupplier {
    JSONataFunction instantiate(List<JSONataFunction> providers, List<DeclaredFunction> declaredFunctions);
}
