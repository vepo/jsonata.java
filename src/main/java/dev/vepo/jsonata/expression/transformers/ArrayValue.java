package dev.vepo.jsonata.expression.transformers;

import static dev.vepo.jsonata.expression.transformers.ValueFactory.json2Value;

import java.util.Objects;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.expression.Node;
import dev.vepo.jsonata.expression.Nodes;

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
                                         .map(ValueFactory::json2Value)
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
}