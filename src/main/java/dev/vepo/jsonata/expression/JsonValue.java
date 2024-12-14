package dev.vepo.jsonata.expression;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.Node;
import dev.vepo.jsonata.exception.JSONataException;

public class JsonValue {
    public interface Value {
        boolean isArray();

        boolean hasField(String fieldName);

        Value get(String fieldName);

        int lenght();

        Value at(int index);

        boolean isEmpty();

        Node toNode();
    }

    private static class ObjectValue implements Value {

        private JsonNode element;

        private ObjectValue(JsonNode element) {
            this.element = element;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean hasField(String fieldName) {
            return element.has(fieldName);
        }

        @Override
        public Value get(String fieldName) {
            return toValue(element.get(fieldName));
        }

        @Override
        public int lenght() {
            return 0;
        }

        @Override
        public Value at(int index) {
            return empty();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Node toNode() {
            return Nodes.object(element);
        }
    }

    private static class ArrayValue implements Value {

        private final ArrayNode element;

        private ArrayValue(ArrayNode element) {
            this.element = element;
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public boolean hasField(String fieldName) {
            return IntStream.range(0, element.size())
                    .anyMatch(i -> element.get(i).has(fieldName));
        }

        @Override
        public Value get(String fieldName) {
            return new GroupedValue(IntStream.range(0, element.size())
                    .mapToObj(element::get)
                    .map(node -> node.get(fieldName))
                    .filter(Objects::nonNull)
                    .map(JsonValue::toValue)
                    .toList());
        }

        @Override
        public int lenght() {
            return element.size();
        }

        @Override
        public Value at(int index) {
            return toValue(element.get(index));
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Node toNode() {
            return Nodes.array(element);
        }
    }

    private static class GroupedValue implements Value {

        private List<Value> elements;

        private GroupedValue(List<Value> elements) {
            this.elements = elements;
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public boolean hasField(String fieldName) {
            return elements.stream().anyMatch(e -> e.hasField(fieldName));
        }

        @Override
        public Value get(String fieldName) {
            return new GroupedValue(elements.stream()
                    .map(e -> e.get(fieldName))
                    .filter(Objects::nonNull)
                    .toList());
        }

        @Override
        public int lenght() {
            return elements.size();
        }

        @Override
        public Value at(int index) {
            return elements.get(index);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Node toNode() {
            return Nodes.group(elements.stream()
                    .map(Value::toNode)
                    .toList());
        }
    }

    private static class EmptyValue implements Value {

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean hasField(String fieldName) {
            return false;
        }

        @Override
        public Value get(String fieldName) {
            return this;
        }

        @Override
        public int lenght() {
            return 0;
        }

        @Override
        public Value at(int index) {
            return this;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Node toNode() {
            return Nodes.empty();
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Value empty() {
        return new EmptyValue();
    }

    private static Value toValue(JsonNode node) {
        if (node.isArray()) {
            return new ArrayValue((ArrayNode) node);
        } else {
            return new ObjectValue(node);
        }
    }

    private Value actual;

    public JsonValue(String value) {
        try {
            actual = toValue(mapper.readTree(value));
        } catch (JsonProcessingException e) {
            throw new JSONataException("Could not load JSON!", e);
        }
    }

    public Node apply(List<Expression> expressions) {
        return expressions.stream()
                .reduce((f1, f2) -> v -> f2.map(f1.map(v)))
                .get()
                .map(actual)
                .toNode();
    }

}
