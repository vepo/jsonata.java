package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata array index selection ({@code [n]}).
 *
 * <p>Supports zero-based and negative indices (counting from the end). Non-array
 * {@code current} is passed through unchanged; out-of-range indices yield empty.
 *
 * @param index the index to select (negative counts from the end)
 */
public record ArrayIndex(int index) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (!current.isArray()) {
            return current;
        }
        if (index >= 0 && index < current.length()) {
            return current.at(index);
        } else if (index < 0 && -index < current.length()) {
            return current.at(current.length() + index);
        } else {
            return Mapping.empty();
        }
    }

}
