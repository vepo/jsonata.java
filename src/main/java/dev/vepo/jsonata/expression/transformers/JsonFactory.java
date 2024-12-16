package dev.vepo.jsonata.expression.transformers;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            throw new JSONataException(String.format("Invalid JSON! content=%s", value), e);
        }
    }

    public static Value stringValue(String value) {
        return new ObjectValue(mapper.getNodeFactory().textNode(value));
    }

    public static Stream<Value> planify(JsonNode value) {
        if (!value.isArray()) {
            return Stream.of(new ObjectValue(value));
        } else {
            return StreamSupport.stream(spliteratorUnknownSize(((ArrayNode) value).elements(), 0), false)
                    .map(ObjectValue::new);
        }
    }

    public static Value numberValue(Integer value) {
        return new ObjectValue(mapper.getNodeFactory().numberNode(value));
    }

    public static Value booleanValue(boolean value) {
        return new ObjectValue(mapper.getNodeFactory().booleanNode(value));
    }

    public static ArrayNode arrayNode(List<JsonNode> elements) {
        var array = mapper.createArrayNode();
        array.addAll(elements);
        return array;
    }

    private JsonFactory() {
    }

    public static class ObjectBuilder {

        private final ObjectNode root;

        private ObjectBuilder(ObjectNode root) {
            this.root = root;
        }

        public Value build() {
            return new ObjectValue(root);
        }

        public void set(String field, Value value) {
            root.set(field, value.toJson());
        }

        public JsonNode root() {
            return root;
        }
    }

    public static ObjectBuilder objectBuilder() {
        return new ObjectBuilder(mapper.createObjectNode());
    }
}
