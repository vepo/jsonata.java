package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.fromString;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.regex.RegExp;

public interface Data {

    public static Data load(String contents) {
        return fromString(contents);
    }

    Data all();

    Data at(int index);

    void forEachChild(Consumer<Data> action);

    Data get(String fieldName);

    boolean hasField(String fieldName);

    default boolean isArray() {
        return false;
    }

    default boolean isList() {
        return false;
    }

    default boolean isEmpty() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default boolean isRegex() {
        return false;
    }

    int length();

    JsonNode toJson();

    JSONataResult toNode();

    default RegExp asRegex() {
        throw new UnsupportedOperationException("Unimplemented method 'asRegex'");
    }
}