package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record AlgebraicJSONataFunction(AlgebraicOperator operator, List<JSONataFunction> rightExpressions) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var right = rightExpressions.stream()
                                    .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                    .map(f -> f.map(original, original).toJson())
                                    .orElse(current.toJson());
        return execute(current.toJson(), right);
    }

    private Data execute(JsonNode left, JsonNode right) {
        if (left.isArray() && right.isArray()) {
            if (left.size() != right.size()) {
                throw new IllegalArgumentException("Arrays must have the same size!");
            }
            return new GroupedData(IntStream.range(0, left.size())
                                            .mapToObj(i -> execute(left.get(i), right.get(i)))
                                            .toList());
        }
        if (left.isArray() && !right.isArray()) {
            return new GroupedData(IntStream.range(0, left.size())
                                            .mapToObj(i -> execute(left.get(i), right))
                                            .toList());
        }

        if (!left.isArray() && right.isArray()) {
            return new GroupedData(IntStream.range(0, right.size())
                                            .mapToObj(i -> execute(left, right.get(i)))
                                            .toList());
        }
        return numberValue(switch (operator) {
            case ADD -> left.asDouble() + right.asDouble();
            case SUBTRACT -> left.asDouble() - right.asDouble();
            case MULTIPLY -> left.asDouble() * right.asDouble();
            case DIVIDE -> left.asDouble() / right.asDouble();
            case MODULO -> left.asDouble() % right.asDouble();
            case POWER -> Math.pow(left.asDouble(), right.asDouble());
        });
    }

}
