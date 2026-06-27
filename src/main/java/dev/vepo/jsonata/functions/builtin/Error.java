package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata built-in {@code $error}. Throws a JSONata exception with the given message. Uses context as the message when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Error(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var message = BuiltInArgs.evaluateOne(providers, original, current).toJson().asText();
        throw new JSONataException(message);
    }
}
