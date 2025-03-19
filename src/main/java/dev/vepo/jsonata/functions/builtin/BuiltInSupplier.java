package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;

@FunctionalInterface
public interface BuiltInSupplier {
    Mapping instantiate(List<Mapping> providers, List<DeclaredFunction> declaredFunctions);
}
