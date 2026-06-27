package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Evaluates a nested function declaration to a first-class function value.
 *
 * <p>JSONata construct: {@code function($x){...}} used as an expression (not immediately
 * invoked). Captures the current focus as closure context for free-variable resolution.
 *
 * @param function the parsed anonymous function
 */
public record FunctionExpression(DeclaredFunction function) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return FunctionValues.wrap(function.asValue().withCapturedContext(current));
    }
}
