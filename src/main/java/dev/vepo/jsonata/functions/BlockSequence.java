package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Executes block statements in order and returns the last expression value.
 */
public record BlockSequence(List<Mapping> statements) implements Mapping {

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
