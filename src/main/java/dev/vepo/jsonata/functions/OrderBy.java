package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

/**
 * JSONata order-by clause: {@code array^(>key1, <key2, ...)}.
 *
 * <p>Sorts the operand array stably by one or more key expressions. Key values must be
 * numbers or strings; descending order is indicated per key in the {@link OrderKey}.
 *
 * @param operand the array (or scalar passed through unchanged) to sort
 * @param keys    sort key expressions with direction flags
 */
public record OrderBy(Mapping operand, List<OrderKey> keys) implements Mapping {

    /**
     * A single sort key with optional descending order.
     *
     * @param expression unevaluated key expression evaluated per array element
     * @param descending {@code true} for descending sort on this key
     */
    public record OrderKey(Mapping expression, boolean descending) {
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var value = operand.map(original, current);
        if (!value.isArray() && !value.isList()) {
            return value;
        }
        var items = new ArrayList<>(value.stream().toList());
        items.sort(comparator(original, current));
        return new GroupedData(items);
    }

    private Comparator<Data> comparator(Data original, Data current) {
        return (left, right) -> {
            for (var key : keys) {
                var leftKey = key.expression().map(original, left);
                var rightKey = key.expression().map(original, right);
                var comp = compareKeyValues(leftKey, rightKey);
                if (comp != 0) {
                    return key.descending() ? -comp : comp;
                }
            }
            return 0;
        };
    }

    private static int compareKeyValues(Data left, Data right) {
        var leftJson = left.toJson();
        var rightJson = right.toJson();
        if (leftJson != null && rightJson != null && leftJson.isNumber() && rightJson.isNumber()) {
            return Double.compare(leftJson.asDouble(), rightJson.asDouble());
        }
        if (leftJson != null && rightJson != null && leftJson.isTextual() && rightJson.isTextual()) {
            return leftJson.asText().compareTo(rightJson.asText());
        }
        throw new JSONataException("Order-by key values must be numbers or strings");
    }
}
