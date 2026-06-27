package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $random}. Returns a pseudo-random number in the range [0, 1).
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Random(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final java.util.Random RANDOM = new java.util.Random();

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return JsonFactory.numberValue(RANDOM.nextDouble());
    }
}
