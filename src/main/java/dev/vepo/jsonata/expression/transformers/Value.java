package dev.vepo.jsonata.expression.transformers;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.expression.Node;
import dev.vepo.jsonata.expression.Nodes;

public interface Value {
    public class ArrayValue implements Value {

        private final ArrayNode element;

        public ArrayValue(ArrayNode element) {
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
                                             .map(Value::json2Value)
                                             .toList());
        }

        @Override
        public int lenght() {
            return element.size();
        }

        @Override
        public Value at(int index) {
            return json2Value(element.get(index));
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Node toNode() {
            return Nodes.array(element);
        }

        @Override
        public JsonNode toJson() {
            return element;
        }

        @Override
        public Value all() {
            return this;
        }
    }

    public class GroupedValue implements Value {

        private List<Value> elements;

        public GroupedValue(List<Value> elements) {
            this.elements = elements;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean hasField(String fieldName) {
            return elements.stream().anyMatch(e -> e.hasField(fieldName));
        }

        @Override
        public Value get(String fieldName) {
            return new GroupedValue(elements.stream()
                                            .filter(v -> v.hasField(fieldName))
                                            .map(e -> e.get(fieldName))
                                            .filter(v -> !v.isEmpty())
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

        @Override
        public JsonNode toJson() {
            var array = JsonValue.mapper.createArrayNode();
            elements.forEach(e -> array.add(e.toJson()));
            return array;
        }

        @Override
        public Value all() {
            return this;
        }
    }

    public class ObjectValue implements Value {

        private JsonNode element;

        public ObjectValue(JsonNode element) {
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
            return json2Value(element.get(fieldName));
        }

        @Override
        public int lenght() {
            return 1;
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

        @Override
        public JsonNode toJson() {
            return element;
        }

        @Override
        public Value all() {
            return new GroupedValue(StreamSupport.stream(spliteratorUnknownSize(element.fields(), 0), false)
                                                 .map(Entry::getValue)
                                                 .map(ObjectValue::new)
                                                 .map(v -> (Value) v).toList());
        }
    }

    public class EmptyValue implements Value {

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

        @Override
        public JsonNode toJson() {
            return null;
        }

        @Override
        public Value all() {
            return this;
        }
    }

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

    boolean isArray();

    boolean hasField(String fieldName);

    Value get(String fieldName);

    int lenght();

    Value at(int index);

    boolean isEmpty();

    Node toNode();

    JsonNode toJson();

    Value all();
}