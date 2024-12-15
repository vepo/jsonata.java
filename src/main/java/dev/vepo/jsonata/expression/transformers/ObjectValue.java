package dev.vepo.jsonata.expression.transformers;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.Node;
import dev.vepo.jsonata.expression.Nodes;

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
        return JsonValue.toValue(element.get(fieldName));
    }

    @Override
    public int lenght() {
        return 0;
    }

    @Override
    public Value at(int index) {
        return JsonValue.empty();
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
}