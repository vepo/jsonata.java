package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata parent context reference ({@code %}).
 *
 * <p>Returns the immediate parent from {@link PathBindings}, or empty when no parent
 * is bound (e.g. at the root of evaluation).
 */
public record ParentReference() implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return PathBindings.parent(1).orElseGet(Mapping::empty);
    }
}
