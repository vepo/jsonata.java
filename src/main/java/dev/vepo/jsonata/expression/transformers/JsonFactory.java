package dev.vepo.jsonata.expression.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.expression.transformers.Value.ArrayValue;
import dev.vepo.jsonata.expression.transformers.Value.ObjectValue;

public class JsonFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Value json2Value(JsonNode node) {
        if (node.isArray()) {
            return new ArrayValue((ArrayNode) node);
        } else {
            return new ObjectValue(node);
        }
    }

    public static Value fromString(String value) {
        try {
            return json2Value(mapper.readTree(value));
        } catch (JsonProcessingException e) {
            throw new JSONataException("Could not load JSON!", e);
        }
    }

    public static Value stringValue(String value) {
        return new ObjectValue(mapper.getNodeFactory().textNode(value));
    }

    private JsonFactory() {
    }
}
