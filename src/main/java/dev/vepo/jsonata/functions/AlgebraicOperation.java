package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;

import java.util.Objects;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record AlgebraicOperation(Mapping left, AlgebraicOperator operator, Mapping right) implements Mapping {

    private static final Logger logger = LoggerFactory.getLogger(AlgebraicOperation.class);
    @Override
    public Data map(Data original, Data current) {
        return execute(left.map(original, current).toJson(), right.map(original, current).toJson());
    }

    private Data execute(JsonNode left, JsonNode right) {
        logger.atDebug().log("Executing {} {} {}", left, operator, right);
        if (Objects.isNull(left) || Objects.isNull(right) || left.isNull() || right.isNull()) {
            return Mapping.empty();
        } else if (left.isArray() && right.isArray()) {
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
