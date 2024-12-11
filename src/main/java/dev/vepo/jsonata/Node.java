package dev.vepo.jsonata;

import java.util.function.IntPredicate;

import com.fasterxml.jackson.databind.JsonNode;

public class Node {

    public static Node empty() {
        return new Node(null);
    }

    private final JsonNode element;

    public Node(JsonNode element) {
        this.element = element;
    }

    public String asText() {
        return element.asText();
    }

    public Integer asInt() {
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
        return element.isNull();
    }

}
