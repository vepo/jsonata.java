package dev.vepo.jsonata.expression.transformers;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.Node;
import dev.vepo.jsonata.expression.Nodes;

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

    @Override
    public JsonNode toJson() {
        var array = JsonValue.mapper.createArrayNode();
        elements.forEach(e -> array.add(e.toJson()));
        return array;
    }
}