package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.json2Value;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.results.JSONataResults;

public class ObjectData implements Data {

    private JsonNode element;

    public ObjectData(JsonNode element) {
        this.element = element;
    }

    @Override
    public Data all() {
        if (element.isObject()) {
            return new GroupedData(StreamSupport.stream(spliteratorUnknownSize(element.fields(), 0), false)
                                                .map(Entry::getValue)
                                                .map(ObjectData::new)
                                                .map(v -> (Data) v)
                                                .toList());
        } else {
            return this;
        }
    }

    @Override
    public Data at(int index) {
        return Mapping.empty();
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
        StreamSupport.stream(spliteratorUnknownSize(element.fields(), 0), false)
                     .map(Entry::getValue)
                     .filter(JsonNode::isObject)
                     .map(ObjectData::new)
                     .forEach(action);
    }

    @Override
    public Data get(String fieldName) {
        return json2Value(element.get(fieldName));
    }

    @Override
    public boolean hasField(String fieldName) {
        return element.has(fieldName);
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public JsonNode toJson() {
        return element;
    }

    @Override
    public JSONataResult toNode() {
        return JSONataResults.object(element);
    }

    @Override
    public Data map(Function<JsonNode, Data> function) {
        return function.apply(element);
    }

    @Override
    public String toString() {
        return String.format("Object [%s]", element);
    }
}