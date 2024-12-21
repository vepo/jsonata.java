package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.arrayNode;

import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.results.JSONataResults;

public class GroupedData implements Data {

    private final List<Data> elements;

    public GroupedData(List<Data> elements) {
        this.elements = elements;
    }

    @Override
    public Data all() {
        return this;
    }

    @Override
    public Data at(int index) {
        return elements.get(index);
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
        elements.forEach(action);
    }

    @Override
    public Data get(String fieldName) {
        return new GroupedData(elements.stream()
                                        .filter(v -> v.hasField(fieldName))
                                        .map(e -> e.get(fieldName))
                                        .filter(v -> !v.isEmpty())
                                        .toList());
    }

    @Override
    public boolean hasField(String fieldName) {
        return elements.stream().anyMatch(e -> e.hasField(fieldName));
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public int length() {
        return elements.size();
    }

    @Override
    public JsonNode toJson() {
        return arrayNode(elements.stream()
                .map(Data::toJson)
                .toList());
    }

    @Override
    public JSONataResult toNode() {
        return JSONataResults.group(elements.stream()
                                            .map(Data::toNode)
                                            .toList());
    }
    
    @Override
    public String toString() {
        return String.format("Grouped [%s]", elements);
    }
}