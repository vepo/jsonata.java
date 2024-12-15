package dev.vepo.jsonata.expression.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ValueFactory {

    public static Value empty() {
        return new EmptyValue();
    }

    public static Value json2Value(JsonNode node) {
        if (node.isArray()) {
            return new ArrayValue((ArrayNode) node);
        } else {
            return new ObjectValue(node);
        }
    }

    private ValueFactory() {
    }
}
