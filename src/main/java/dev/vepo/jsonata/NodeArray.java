package dev.vepo.jsonata;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class NodeArray implements NodeList {

    private final ArrayNode element;

    public NodeArray(ArrayNode element) {
        this.element = element;
    }

    @Override
    public List<String> asText() {
        return IntStream.range(0, element.size())
                .mapToObj(i -> element.get(i).asText())
                .toList();
    }

    @Override
    public NodeObject asObject() {
        throw new IllegalStateException("This is not an object!");
    }

    @Override
    public boolean isEmpty() {
        return element.size() == 0;
    }

    @Override
    public boolean hasField(String field) {
        return IntStream.range(0, element.size())
                .anyMatch(i -> element.get(i).has(field));
    }

    @Override
    public Node get(String field) {
        return new NodeGroup(IntStream.range(0, element.size())
                .mapToObj(element::get)
                .map(node -> node.get(field))
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public int lenght() {
        return element.size();
    }

    @Override
    public Node at(int index) {
        return new NodeObject(element.get(index));
    }

    @Override
    public NodeList asList() {
        return this;
    }

}
