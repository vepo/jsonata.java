package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Evaluates a nested function declaration to a first-class function value.
 */
public record FunctionExpression(DeclaredFunction function) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return FunctionValues.wrap(function.asValue().withCapturedContext(current));
    }
}
