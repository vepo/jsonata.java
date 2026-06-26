package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Marker wrapping the left-hand path operand before {@code @$var}.
 * {@link MappingJoin} binds the focus variable while preserving the outer context.
 */
public record ContextBind(Mapping operand, String variableName) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return operand.map(original, current);
    }
}
