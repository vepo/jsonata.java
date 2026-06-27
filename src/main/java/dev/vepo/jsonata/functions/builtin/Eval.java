package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.MappingParser;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata built-in {@code $eval}. Parses and evaluates a JSONata expression string. Uses context as the expression when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Eval(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var expr = BuiltInArgs.evaluateOne(providers, original, current).toJson().asText();
        var mappings = MappingParser.parse(expr);
        if (mappings.isEmpty()) {
            return Mapping.empty();
        }
        return mappings.get(0).map(original, current);
    }
}
