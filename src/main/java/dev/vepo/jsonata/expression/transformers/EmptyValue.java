package dev.vepo.jsonata.expression.transformers;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.Node;
import dev.vepo.jsonata.expression.Nodes;

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
}