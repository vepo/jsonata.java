package dev.vepo.jsonata.functions.json;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.EvaluationContext;
import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;
import dev.vepo.jsonata.functions.data.ObjectData;
import dev.vepo.jsonata.functions.data.RegexData;

/**
 * Infrastructure adapter between Jackson {@link JsonNode} and domain {@link Data}.
 * <p>
 * Parses JSON text, constructs literal values, and wraps nodes as {@link ObjectData}
 * or {@link ArrayData} tagged with the active session {@link DataInspector} from
 * {@link EvaluationContext}. Domain code should depend on {@link Data}, not on this type.
 * <p>
 * Static initialization registers {@link MutableJacksonDataInspector} as the default
 * inspector via {@link DataInspectors#setDefault(DataInspector)}.
 */
public class JsonFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        DataInspectors.setDefault(MutableJacksonDataInspector.INSTANCE);
    }

    /**
     * Triggers static initialization (default {@link DataInspector} registration).
     * Call from embedders that need explicit bootstrap ordering.
     */
    public static void bootstrap() {
        // Triggers static initialization of default DataInspector registration.
    }

    private static DataInspector inspector() {
        return EvaluationContext.currentInspector();
    }

    /**
     * Wraps a Jackson node as domain {@link Data}.
     *
     * @param node JSON value; arrays become {@link ArrayData}, all other shapes {@link ObjectData}
     * @return domain wrapper using the current evaluation inspector
     */
    public static Data json2Value(JsonNode node) {
        return json2Value(node, inspector());
    }

    /**
     * Wraps a Jackson node as domain {@link Data} with an explicit inspector.
     *
     * @param node JSON value; arrays become {@link ArrayData}, all other shapes {@link ObjectData}
     * @param dataInspector backing-store adapter to attach to the result
     * @return domain wrapper tagged with {@code dataInspector}
     */
    public static Data json2Value(JsonNode node, DataInspector dataInspector) {
        if (node.isArray()) {
            return new ArrayData((ArrayNode) node, dataInspector);
        } else {
            return new ObjectData(node, dataInspector);
        }
    }

    /**
     * Parses JSON text into domain {@link Data}.
     *
     * @param value JSON document text
     * @return root value as {@link ObjectData} or {@link ArrayData}
     * @throws JSONataException if {@code value} is not valid JSON
     */
    public static Data fromString(String value) {
        try {
            return json2Value(mapper.readTree(value));
        } catch (JsonProcessingException e) {
            throw new JSONataException(String.format("Invalid JSON! content=%s", value), e);
        }
    }

    /**
     * @param value string literal
     * @return scalar {@link ObjectData} for {@code value}
     */
    public static Data stringValue(String value) {
        return new ObjectData(mapper.getNodeFactory().textNode(value), inspector());
    }

    /**
     * Flattens a JSON value into a stream of object-shaped {@link Data} for plan-style evaluation.
     *
     * @param value JSON node; non-arrays yield a single-element stream
     * @return stream of {@link ObjectData} elements
     */
    public static Stream<Data> planify(JsonNode value) {
        if (!value.isArray()) {
            return Stream.of(new ObjectData(value, inspector()));
        } else {
            return StreamSupport.stream(spliteratorUnknownSize(((ArrayNode) value).elements(), 0), false)
                                .map(node -> new ObjectData(node, inspector()));
        }
    }

    /**
     * @param values string elements
     * @return {@link ArrayData} containing the given strings
     */
    public static Data arrayValue(String[] values) {
        var array = mapper.createArrayNode();
        Stream.of(values)
              .forEach(array::add);
        return new ArrayData(array, inspector());
    }

    /**
     * @param values integer elements
     * @return {@link ArrayData} containing the given integers
     */
    public static Data arrayValue(int[] values) {
        var array = mapper.createArrayNode();
        IntStream.of(values)
              .forEach(array::add);
        return new ArrayData(array, inspector());
    }

    /**
     * @param value integer literal
     * @return scalar {@link ObjectData}
     */
    public static Data numberValue(Integer value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value), inspector());
    }

    /**
     * @param value decimal literal
     * @return scalar {@link ObjectData}
     */
    public static Data numberValue(BigDecimal value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value), inspector());
    }

    /**
     * @param value long literal
     * @return scalar {@link ObjectData}
     */
    public static Data numberValue(Long value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value), inspector());
    }

    /**
     * @param value float literal
     * @return scalar {@link ObjectData}
     */
    public static Data numberValue(Float value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value), inspector());
    }

    /**
     * @param value double literal
     * @return scalar {@link ObjectData}
     */
    public static Data numberValue(Double value) {
        return new ObjectData(mapper.getNodeFactory().numberNode(value), inspector());
    }

    /**
     * @param value boolean literal (nullable)
     * @return scalar {@link ObjectData}
     */
    public static Data booleanValue(Boolean value) {
        return new ObjectData(mapper.getNodeFactory().booleanNode(value), inspector());
    }

    /**
     * @param value boolean literal
     * @return scalar {@link ObjectData}
     */
    public static Data booleanValue(boolean value) {
        return new ObjectData(mapper.getNodeFactory().booleanNode(value), inspector());
    }

    /**
     * @param text regex literal source (including JSONata/JS delimiters)
     * @return {@link RegexData} for the pattern
     */
    public static Data regex(String text) {
        return new RegexData(mapper.getNodeFactory().textNode(text));
    }

    /**
     * Builds a Jackson {@link ArrayNode} from a list of child nodes.
     *
     * @param elements JSON child nodes
     * @return new array node containing all elements
     */
    public static ArrayNode arrayNode(List<JsonNode> elements) {
        var array = mapper.createArrayNode();
        array.addAll(elements);
        return array;
    }

    private JsonFactory() {}

    /**
     * Mutable builder for constructing JSON objects during evaluation (object constructors, aggregates).
     *
     * @param root underlying object node being populated
     * @param groupRecordsInArray when {@code true}, duplicate field names are coerced into arrays
     */
    public static record ObjectBuilder(ObjectNode root, boolean groupRecordsInArray) {

        private ObjectBuilder(ObjectNode root) {
            this(root, false);
        }

        /**
         * @return {@link ObjectData} wrapping the built object with the current session inspector
         */
        public Data build() {
            return new ObjectData(root, inspector());
        }

        /**
         * Sets a field, applying grouping/merge rules when {@link #groupRecordsInArray()} is enabled.
         *
         * @param field property name
         * @param value field value
         */
        public void set(String field, Data value) {
            set(field, value, false);
        }

        /**
         * Sets a field with optional deep merge when both old and new values are objects.
         *
         * @param field property name
         * @param value field value
         * @param merge when {@code true} and an object field already exists, merge object fields
         */
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

        /**
         * Appends {@code value} to an array field, creating the array if the field is absent.
         *
         * @param field property name
         * @param value element to append
         */
        public void add(String field, Data value) {
            if (root.has(field)) {
                ((ArrayNode) root.get(field)).add(value.toJson());
            } else {
                var arr = root.arrayNode();
                arr.add(value.toJson());
                root.set(field, arr);
            }
        }

        /**
         * Copies all fields from {@code current} into this builder's root object.
         *
         * @param current source object whose fields are merged in
         */
        public void fill(Data current) {
            current.toJson()
                   .fields()
                   .forEachRemaining(entry -> root.set(entry.getKey(), entry.getValue()));
        }

        /**
         * @param key property name
         * @return {@code true} if the root object already has {@code key}
         */
        public boolean hasValue(String key) {
            return root.has(key);
        }
    }

    /**
     * @return new object builder with default grouping disabled
     */
    public static ObjectBuilder objectBuilder() {
        return new ObjectBuilder(mapper.createObjectNode());
    }

    /**
     * @param groupRecordsInArray when {@code true}, duplicate keys are accumulated into arrays
     * @return new object builder with the given grouping mode
     */
    public static ObjectBuilder objectBuilder(boolean groupRecordsInArray) {
        return new ObjectBuilder(mapper.createObjectNode(), groupRecordsInArray);
    }
}
