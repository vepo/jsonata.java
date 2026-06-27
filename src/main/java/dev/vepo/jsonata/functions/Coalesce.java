package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata coalesce operator ({@code ??}): returns the left operand unless empty.
 *
 * <p>Unlike {@link DefaultOperator} ({@code ?:}), coalesce only checks for
 * empty/null — not effective boolean truthiness.
 *
 * @param left  the primary operand
 * @param right the fallback when {@code left} is empty or null
 */
public record Coalesce(Mapping left, Mapping right) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var leftValue = left.map(original, current);
        if (leftValue == null || leftValue.isEmpty()) {
            return right.map(original, current);
        }
        return leftValue;
    }
}
