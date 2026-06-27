package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata string concatenation ({@code &} operator).
 *
 * <p>Evaluates both operands and concatenates their string representations; null or
 * missing JSON nodes contribute an empty string.
 *
 * @param firstValue  the left-hand string operand
 * @param secondValue the right-hand string operand
 */
public record Concatenation(Mapping firstValue, Mapping secondValue) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return stringValue(safeAsText(firstValue.map(original, current)) + safeAsText(secondValue.map(original, current)));
    }

    private static String safeAsText(Data value) {
        return Optional.ofNullable(value.toJson())
                       .map(JsonNode::asText)
                       .orElse("");
    }
}
