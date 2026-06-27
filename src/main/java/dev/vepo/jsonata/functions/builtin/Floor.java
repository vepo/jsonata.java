package dev.vepo.jsonata.functions.builtin;

import java.math.RoundingMode;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $floor}. Returns the largest integer not greater than the argument. Uses context when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Floor(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        var num = BuiltInHelper.toNumber(arg);
        return num != null ? JsonFactory.numberValue(num.setScale(0, RoundingMode.FLOOR)) : Mapping.empty();
    }
}
