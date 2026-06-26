package dev.vepo.jsonata.conformance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Deep JSON comparison for conformance expected vs actual results.
 */
public final class JsonComparison {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonComparison() {
    }

    public static boolean equivalent(JsonNode expected, JsonNode actual) {
        if (expected == null || expected.isNull()) {
            return actual == null || actual.isNull();
        }
        if (actual == null || actual.isNull()) {
            return false;
        }
        if (expected.isNumber() && actual.isNumber()) {
            return expected.decimalValue().compareTo(actual.decimalValue()) == 0;
        }
        if (expected.getNodeType() != actual.getNodeType()) {
            return false;
        }
        if (expected.isArray()) {
            if (expected.size() != actual.size()) {
                return false;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!equivalent(expected.get(i), actual.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (expected.isObject()) {
            if (expected.size() != actual.size()) {
                return false;
            }
            var fieldNames = expected.fieldNames();
            while (fieldNames.hasNext()) {
                var name = fieldNames.next();
                if (!actual.has(name) || !equivalent(expected.get(name), actual.get(name))) {
                    return false;
                }
            }
            return true;
        }
        return expected.equals(actual);
    }

    public static JsonNode parseResult(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON result: " + json, e);
        }
    }

    public static JsonNode resultToJson(dev.vepo.jsonata.JSONataResult result) {
        if (result.isEmpty()) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (result.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }
        var text = result.asText();
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            if (result.multi().asText().size() > 1) {
                var arr = MAPPER.createArrayNode();
                result.multi().asText().forEach(t -> arr.add(parseOrText(t)));
                return arr;
            }
            return JsonNodeFactory.instance.textNode(text);
        }
    }

    private static JsonNode parseOrText(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            return JsonNodeFactory.instance.textNode(text);
        }
    }

    public static ObjectNode bindingsToEnvironment(com.fasterxml.jackson.databind.node.ObjectNode bindings) {
        return bindings == null ? MAPPER.createObjectNode() : bindings;
    }
}
