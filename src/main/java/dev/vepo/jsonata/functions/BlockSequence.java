package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata block: a sequence of statements ending in a value expression.
 *
 * <p>Executes each statement in order (assignments produce empty results) and returns
 * the value of the last statement. An empty block yields {@link Mapping#empty()}.
 *
 * @param statements ordered block statements and the final expression
 */
public record BlockSequence(List<Mapping> statements) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (statements.isEmpty()) {
            return Mapping.empty();
        }
        Data result = Mapping.empty();
        for (var statement : statements) {
            result = statement.map(original, current);
        }
        return result;
    }
}
