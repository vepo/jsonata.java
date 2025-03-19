package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.arrayNode;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
    public boolean isList() {
        return true;
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
    public Data map(Function<JsonNode, Data> function) {
        return new GroupedData(elements.stream()
                                      .map(e -> e.map(function))
                                      .toList());
    }

    @Override
    public Stream<Data> stream() {
        return elements.stream();
    }
    
    @Override
    public String toString() {
        return String.format("Grouped [%s]", elements);
    }
}