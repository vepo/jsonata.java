package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.json2Value;
import static dev.vepo.jsonata.results.JSONataResults.array;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.json.JsonFactory;

public class ArrayData implements Data {

    private final ArrayNode element;

    public ArrayData(ArrayNode element) {
        this.element = element;
    }

    @Override
    public Data all() {
        return this;
    }

    @Override
    public Data at(int index) {
        return json2Value(element.get(index));
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
        for (int i = 0; i < element.size(); ++i) {
            action.accept(json2Value(element.get(i)));
        }
    }

    private Stream<JsonNode> toStream(ArrayNode node) {
        return IntStream.range(0, node.size())
                        .mapToObj(node::get);
    }

    @Override
    public Data get(String fieldName) {
        return new GroupedData(toStream(element).map(node -> node.get(fieldName))
                                                .filter(Objects::nonNull)
                                                .flatMap(node -> node.isArray() ? toStream((ArrayNode) node) : Stream.of(node))
                                                .map(JsonFactory::json2Value)
                                                .toList());
    }

    @Override
    public boolean hasField(String fieldName) {
        return IntStream.range(0, element.size())
                        .anyMatch(i -> element.get(i).has(fieldName));
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public int length() {
        return element.size();
    }

    @Override
    public JsonNode toJson() {
        return element;
    }

    @Override
    public JSONataResult toNode() {
        return array(element);
    }

    @Override
    public Data map(Function<JsonNode, Data> function) {
        return new GroupedData(toStream(element).map(function)
                                                .toList());
    }

    @Override
    public String toString() {
        return String.format("Grouped [%s]", element);
    }
}