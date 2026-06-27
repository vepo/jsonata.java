package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Marker wrapping the left-hand path operand before {@code @$var}.
 *
 * <p>{@link MappingJoin} recognizes this wrapper and binds the focus variable while
 * preserving the outer evaluation context for the right-hand operand.
 *
 * @param operand      the path expression producing the value to bind
 * @param variableName focus variable name (without {@code @} prefix)
 */
public record ContextBind(Mapping operand, String variableName) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return operand.map(original, current);
    }
}
