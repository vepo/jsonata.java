package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.regex.RegExp;

public class RegexData implements Data {

    private final JsonNode node;

    public RegexData(JsonNode node) {
        this.node = node;
    }

    @Override
    public Data all() {
        throw new UnsupportedOperationException("Unimplemented method 'all'");
    }

    @Override
    public Data at(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'at'");
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
        throw new UnsupportedOperationException("Unimplemented method 'forEachChild'");
    }

    @Override
    public Data get(String fieldName) {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    @Override
    public boolean hasField(String fieldName) {
        throw new UnsupportedOperationException("Unimplemented method 'hasField'");
    }

    @Override
    public int length() {
        throw new UnsupportedOperationException("Unimplemented method 'length'");
    }

    @Override
    public JsonNode toJson() {
        throw new UnsupportedOperationException("Unimplemented method 'toJson'");
    }

    @Override
    public JSONataResult toNode() {
        throw new UnsupportedOperationException("Unimplemented method 'toNode'");
    }

    @Override
    public boolean isRegex() {
        return true;
    }

    @Override
    public RegExp asRegex() {
        return new RegExp(node.asText());
    }

    @Override
    public String toString() {
        return String.format("RegexData [%s]", node);
    }

}
