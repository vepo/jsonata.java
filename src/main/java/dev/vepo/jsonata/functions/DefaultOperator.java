package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Elvis / default operator: {@code lhs ?: rhs} returns lhs if effective boolean true, else rhs.
 */
public record DefaultOperator(Mapping left, Mapping right) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var leftValue = left.map(original, current);
        if (effectiveBoolean(leftValue)) {
            return leftValue;
        }
        return right.map(original, current);
    }

    private static boolean effectiveBoolean(Data data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        var json = data.toJson();
        if (json == null) {
            return false;
        }
        if (json.isBoolean()) {
            return json.asBoolean();
        }
        if (json.isNumber()) {
            return json.decimalValue().signum() != 0;
        }
        if (json.isTextual()) {
            return !json.asText().isEmpty();
        }
        if (json.isNull()) {
            return false;
        }
        if (data.isArray() || data.isList()) {
            if (data.length() == 0) {
                return false;
            }
            if (data.length() == 1) {
                return effectiveBoolean(data.at(0));
            }
            return true;
        }
        if (data.isObject()) {
            return json.size() > 0;
        }
        if (FunctionValues.isFunction(data)) {
            return false;
        }
        return true;
    }
}
