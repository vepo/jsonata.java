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
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Domain {@link Data} backed by a JSON array ({@link ArrayNode}).
 * <p>
 * Created by {@link JsonFactory} during JSON load and literal construction.
 * Field projection ({@link #get(String)}) maps over elements and flattens nested arrays.
 * The attached {@link DataInspector} governs whether underlying nodes may be mutated in place.
 */
public class ArrayData implements Data {

    private final ArrayNode element;
    private final DataInspector inspector;

    /**
     * Wraps {@code element} with the registered default {@link DataInspector}.
     *
     * @param element Jackson array node; must not be {@code null}
     */
    public ArrayData(ArrayNode element) {
        this(element, DataInspectors.defaultInspector());
    }

    /**
     * Wraps {@code element} with the given session inspector.
     *
     * @param element Jackson array node; must not be {@code null}
     * @param inspector backing-store adapter for copy and transform operations
     */
    public ArrayData(ArrayNode element, DataInspector inspector) {
        this.element = element;
        this.inspector = inspector;
    }

    /** {@inheritDoc} */
    @Override
    public Data all() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        return json2Value(element.get(index), inspector);
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
        for (int i = 0; i < element.size(); ++i) {
            action.accept(json2Value(element.get(i), inspector));
        }
    }

    private Stream<JsonNode> toStream(ArrayNode node) {
        return IntStream.range(0, node.size())
                        .mapToObj(node::get);
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        return new GroupedData(toStream(element).map(node -> node.get(fieldName))
                                                .filter(Objects::nonNull)
                                                .flatMap(node -> node.isArray() ? toStream((ArrayNode) node) : Stream.of(node))
                                                .map(node -> json2Value(node, inspector))
                                                .toList());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        return IntStream.range(0, element.size())
                        .anyMatch(i -> element.get(i).has(fieldName));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isArray() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return element.size();
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode toJson() {
        return element;
    }

    /** {@inheritDoc} */
    @Override
    public JSONataResult toNode() {
        return array(element);
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Function<JsonNode, Data> function) {
        return new GroupedData(toStream(element).map(function)
                                                .toList());
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Data> stream() {
        return toStream(element).map(node -> json2Value(node, inspector));
    }

    /** {@inheritDoc} */
    @Override
    public DataInspector inspector() {
        return inspector;
    }

    @Override
    public String toString() {
        return String.format("Grouped [%s]", element);
    }
}
