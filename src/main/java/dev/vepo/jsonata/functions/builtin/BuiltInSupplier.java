package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;

/**
 * Factory for built-in {@link Mapping} instances from parse-tree argument and function providers.
 */
@FunctionalInterface
public interface BuiltInSupplier {

    /**
     * Creates a built-in mapping bound to the given argument providers and declared functions.
     *
     * @param providers argument expression mappings from the parse tree
     * @param declaredFunctions function-valued parameters from the parse tree
     * @return a {@link Mapping} ready for evaluation
     */
    Mapping instantiate(List<Mapping> providers, List<DeclaredFunction> declaredFunctions);
}
