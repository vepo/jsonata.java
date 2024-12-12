package dev.vepo.jsonata;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Node {

    public static Node empty() {
        return new Node(null);
    }

    private final JsonNode element;

    public Node(JsonNode element) {
        this.element = element;
    }

    public String asText() {
        if (element.isTextual()) {
            return element.asText();
        } else {
            return element.toString();
        }
    }

    public boolean asBoolean() {
        Objects.requireNonNull(element, "Element is null!!");
        return element.asBoolean();
    }

    public int asInt() {
        Objects.requireNonNull(element, "Element is null!!");
        return element.asInt();
    }

    public boolean isEmpty() {
        return element == null;
    }

    public boolean hasField(String field) {
        return element != null && element.has(field);
    }

    public Node get(String field) {
        return new Node(element.get(field));
    }

    public boolean isNull() {
        // For null value, the element should exists
        return Objects.nonNull(element) && element.isNull();
    }

    public boolean isArray() {
        Objects.requireNonNull(element, "Element is null!!");
        return element.isArray();
    }

    public int lenght() {
        Objects.requireNonNull(element, "Element is null!!");
        return element.isArray() ? ((ArrayNode) element).size() : 1;
    }

    public Node at(Integer index) {
        Objects.requireNonNull(element, "Element is null!!");
        return element.isArray() ? new Node(((ArrayNode) element).get(index)) : Node.empty();
    }

}
