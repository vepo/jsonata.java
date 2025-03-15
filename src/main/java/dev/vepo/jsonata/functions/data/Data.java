package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;

import static dev.vepo.jsonata.functions.json.JsonFactory.fromString;

public interface Data {

    public static Data load(String contents) {
        return fromString(contents);
    }

    Data all();

    Data at(int index);

    void forEachChild(Consumer<Data> action);

    Data get(String fieldName);

    boolean hasField(String fieldName);

    boolean isArray();

    boolean isList();

    boolean isEmpty();

    boolean isObject();

    int length();

    JsonNode toJson();

    JSONataResult toNode();
}