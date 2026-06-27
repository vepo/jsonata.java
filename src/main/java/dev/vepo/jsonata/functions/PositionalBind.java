package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Marker wrapping the left-hand path operand before {@code #$var}.
 *
 * <p>{@link MappingJoin} recognizes this wrapper and binds the positional index variable
 * when composing with the next path step.
 *
 * @param operand      the path expression producing the sequence to iterate
 * @param variableName index variable name (without {@code #} prefix)
 */
public record PositionalBind(Mapping operand, String variableName) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return operand.map(original, current);
    }
}
