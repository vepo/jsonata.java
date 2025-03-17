package dev.vepo.jsonata.functions.json;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.ObjectData;
import dev.vepo.jsonata.functions.data.RegexData;

public class JsonFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Data json2Value(JsonNode node) {
        if (node.isArray()) {
            return new ArrayData((ArrayNode) node);
        } else {
            return new ObjectData(node);
        }
    }

    public static Data fromString(String value) {
        try {
            return json2Value(mapper.readTree(value));
        } catch (JsonProcessingException e) {
            throw new JSONataException(String.format("Invalid JSON! content=%s", value), e);
        }
    }

    public static Data stringValue(String value) {
        return new ObjectData(mapper.getNodeFactory().textNode(value));
    }

    public static Stream<Data> planify(JsonNode value) {
        if (!value.isArray()) {
            return Stream.of(new ObjectData(value));
        } else {
            return StreamSupport.stream(spliteratorUnknownSize(((ArrayNode) value).elements(), 0), false)
                                .map(ObjectData::new);
        }
    }

    public static Data numberValue(Integer value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value));
    }

    public static Data numberValue(BigDecimal value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value));
    }

    public static Data numberValue(Long value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value));
    }

    public static Data numberValue(Float value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value));
    }

    public static Data numberValue(Double value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value));
    }

    public static Data booleanValue(Boolean value) {
        return new ObjectData(mapper.getNodeFactory().booleanNode(value));
    }

    public static Data booleanValue(boolean value) {
        return new ObjectData(mapper.getNodeFactory().booleanNode(value));
    }

    public static Data regex(String text) {
        return new RegexData(mapper.getNodeFactory().textNode(text));
    }

    public static ArrayNode arrayNode(List<JsonNode> elements) {
        var array = mapper.createArrayNode();
        array.addAll(elements);
        return array;
    }

    private JsonFactory() {}

    public static record ObjectBuilder(ObjectNode root, boolean groupRecordsInArray) {

        private ObjectBuilder(ObjectNode root) {
            this(root, false);
        }

        public Data build() {
            return new ObjectData(root);
        }

        public void set(String field, Data value) {
            set(field, value, false);
        }

        public void set(String field, Data value, boolean merge) {
            if (groupRecordsInArray && root.has(field)) {
                var previousValue = root.get(field);
                if (merge && previousValue.isObject()) {
                    merge((ObjectNode) previousValue, (ObjectNode) value.toJson());
                } else if (previousValue.isArray()) {
                    var arrValue = value.toJson();
                    if (arrValue.isArray()) {
                        ((ArrayNode) previousValue).addAll((ArrayNode) arrValue);
                    } else {
                        ((ArrayNode) previousValue).add(arrValue);
                    }
                } else {
                    var arr = root.arrayNode();
                    arr.add(previousValue);
                    arr.add(value.toJson());
                    root.set(field, arr);
                }
            } else {
                root.set(field, value.toJson());
            }
        }

        private void merge(ObjectNode prevValue, ObjectNode newValue) {
            newValue.fields()
                    .forEachRemaining(field -> {
                        var fieldName = field.getKey();
                        var fieldValue = field.getValue();
                        if (prevValue.has(fieldName)) {
                            var prevField = prevValue.get(fieldName);
                            if (prevField.isObject()) {
                                merge((ObjectNode) prevField, (ObjectNode) fieldValue);
                            } else if (prevField.isArray()) {
                                if (fieldValue.isArray()) {
                                    ((ArrayNode) prevField).addAll((ArrayNode) fieldValue);
                                } else {
                                    ((ArrayNode) prevField).add(fieldValue);
                                }
                            } else {
                                var arr = root.arrayNode();
                                arr.add(prevField);
                                arr.add(fieldValue);
                                prevValue.set(fieldName, arr);
                            }
                        } else {
                            prevValue.set(fieldName, fieldValue);
                        }
                    });
        }

        public void add(String field, Data value) {
            if (root.has(field)) {
                ((ArrayNode) root.get(field)).add(value.toJson());
            } else {
                var arr = root.arrayNode();
                arr.add(value.toJson());
                root.set(field, arr);
            }
        }

        public void fill(Data current) {
            current.toJson()
                   .fields()
                   .forEachRemaining(entry -> root.set(entry.getKey(), entry.getValue()));
        }

        public boolean hasValue(String key) {
            return root.has(key);
        }
    }

    public static ObjectBuilder objectBuilder() {
        return new ObjectBuilder(mapper.createObjectNode());
    }

    public static ObjectBuilder objectBuilder(boolean groupRecordsInArray) {
        return new ObjectBuilder(mapper.createObjectNode(), groupRecordsInArray);
    }
}
