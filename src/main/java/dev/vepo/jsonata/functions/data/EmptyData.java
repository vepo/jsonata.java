package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.results.JSONataResults;

public class EmptyData implements Data {

    @Override
    public Data all() {
        return this;
    }

    @Override
    public Data at(int index) {
        return this;
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
        // nothing! This is empty
    }

    @Override
    public Data get(String fieldName) {
        return this;
    }

    @Override
    public boolean hasField(String fieldName) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public JsonNode toJson() {
        return null;
    }

    @Override
    public JSONataResult toNode() {
        return JSONataResults.empty();
    }

    @Override
    public Data map(Function<JsonNode, Data> function) {
        return this;
    }

    @Override
    public String toString() {
        return "Empty []";
    }
}