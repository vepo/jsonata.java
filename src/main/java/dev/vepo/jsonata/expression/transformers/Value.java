package dev.vepo.jsonata.expression.transformers;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.Node;

public interface Value {
    boolean isArray();

    boolean hasField(String fieldName);

    Value get(String fieldName);

    int lenght();

    Value at(int index);

    boolean isEmpty();

    Node toNode();

    JsonNode toJson();
}