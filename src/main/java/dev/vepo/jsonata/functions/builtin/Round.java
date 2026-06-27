package dev.vepo.jsonata.functions.builtin;

import java.math.RoundingMode;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $round}. Rounds a number to an integer or to a given precision. Uses context when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Round(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 0, 2, true, original, current);
        var num = BuiltInHelper.toNumber(args.get(0));
        if (num == null) {
            return Mapping.empty();
        }
        if (args.size() == 2) {
            var precision = BuiltInHelper.toNumber(args.get(1));
            if (precision == null) {
                return Mapping.empty();
            }
            return JsonFactory.numberValue(num.setScale(precision.intValue(), RoundingMode.HALF_UP));
        }
        return JsonFactory.numberValue(num.setScale(0, RoundingMode.HALF_UP));
    }
}
