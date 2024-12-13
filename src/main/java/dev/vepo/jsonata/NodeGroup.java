package dev.vepo.jsonata;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

public class NodeGroup implements NodeList {

    private List<JsonNode> elements;

    public NodeGroup(List<JsonNode> elements) {
        this.elements = elements;
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public List<String> asText() {
        return elements.stream()
                .map(JsonNode::asText)
                .toList();
    }

    @Override
    public NodeObject asObject() {
        throw new IllegalStateException("This is not an object!");
    }

    @Override
    public boolean hasField(String field) {
        return elements.stream().anyMatch(e -> e.has(field));
    }

    @Override
    public Node get(String field) {
        return new NodeGroup(elements.stream()
                .map(e -> e.get(field))
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isArray() {
        return elements.stream().anyMatch(e -> e.isArray());
    }

    @Override
    public int lenght() {
        return elements.size();
    }

    @Override
    public Node at(int index) {
        return new NodeObject(elements.get(index));
    }

    @Override
    public NodeList asList() {
        return this;
    }

}
