package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata boolean expression ({@code and}, {@code or}).
 *
 * <p>Both operands are evaluated and coerced to boolean JSON nodes before applying
 * short-circuit semantics at the JSON level.
 *
 * @param left     the left-hand operand
 * @param operator the boolean operator
 * @param right    the right-hand operand
 */
public record BooleanExpression(Mapping left, BooleanOperator operator, Mapping right) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return booleanValue(compare(left.map(original, current).toJson(),
                                    right.map(original, current).toJson()));
    }

    private boolean compare(JsonNode left, JsonNode right) {
        return switch (operator) {
            case AND -> left.asBoolean() && right.asBoolean();
            case OR -> left.asBoolean() || right.asBoolean();
        };
    }

}
