package dev.vepo.jsonata.functions;

import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata conditional expression ({@code test ? then : else}).
 *
 * <p>Evaluates {@code testProvider}; when the result is boolean {@code true}, returns
 * {@code trueValueProvider}; otherwise evaluates the optional {@code falseValueProvider}
 * or returns empty when absent.
 *
 * @param testProvider       the condition expression
 * @param trueValueProvider  the value when the condition is true
 * @param falseValueProvider optional value when the condition is false
 */
public record InlineIf(Mapping testProvider, Mapping trueValueProvider, Optional<Mapping> falseValueProvider)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var testValue = testProvider.map(original, current).toJson();
        if (testValue.isBoolean() && testValue.asBoolean()) {
            return trueValueProvider.map(original, current);
        } else {
            return falseValueProvider.map(fn -> fn.map(original, current))
                                     .orElseGet(Mapping::empty);
        }
    }

}
