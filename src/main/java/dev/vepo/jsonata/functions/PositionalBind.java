package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Marker wrapping the left-hand path operand before {@code #$var}.
 * {@link MappingJoin} applies index bindings when composing with the next step.
 */
public record PositionalBind(Mapping operand, String variableName) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return operand.map(original, current);
    }
}
