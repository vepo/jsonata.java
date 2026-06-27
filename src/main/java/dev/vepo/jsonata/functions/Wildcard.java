package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata wildcard field selection ({@code *} on objects).
 *
 * <p>When {@code current} is a non-empty object, returns all direct child values as a
 * grouped sequence; otherwise returns {@code current} unchanged.
 */
public record Wildcard() implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (!current.isEmpty() && current.isObject()) {
            return current.all();
        } else {
            return current;
        }
    }

}
