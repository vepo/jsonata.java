package dev.vepo.jsonata;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class NodeObject implements Node {

    private final JsonNode element;

    public NodeObject(JsonNode element) {
        Objects.requireNonNull(element, "Element is null!!");
        this.element = element;
    }

    public String asText() {
        if (element.isTextual()) {
            return element.asText();
        } else {
            return element.toString();
        }
    }

    @Override
    public NodeObject asObject() {
        return this;
    }

    public boolean asBoolean() {
        return element.asBoolean();
    }

    public int asInt() {
        return element.asInt();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean hasField(String field) {
        return element.has(field);
    }

    @Override
    public Node get(String field) {
        var innerElement = element.get(field);
        if (innerElement.isArray()) {
            return new NodeArray((ArrayNode) innerElement);
        } else {
            return new NodeObject(element.get(field));
        }
    }

    @Override
    public boolean isNull() {
        return element.isNull();
    }

    @Override
    public boolean isArray() {
        return element.isArray();
    }

    @Override
    public int lenght() {
        return element.isArray() ? ((ArrayNode) element).size() : 1;
    }

    @Override
    public Node at(int index) {
        return element.isArray() ? new NodeObject(((ArrayNode) element).get(index)) : Node.emptyNode();
    }

    @Override
    public NodeList asList() {
        if (element.isArray()) {
            return new NodeArray((ArrayNode) element);
        } else {
            throw new IllegalStateException("Element is not an array!!!");
        }
    }

}
