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
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;
import dev.vepo.jsonata.results.JSONataResults;

/**
 * Domain {@link Data} for a JSON object or scalar {@link JsonNode}.
 * <p>
 * Objects support field navigation and wildcard collection via {@link #all()}.
 * Scalars (strings, numbers, booleans, {@code null}) report {@link #length()} {@code 1}
 * and return {@link Mapping#empty()} for array indexing. Created by
 * {@link dev.vepo.jsonata.functions.json.JsonFactory} for loaded JSON and literals.
 */
public class ObjectData implements Data {

    private final JsonNode element;
    private final DataInspector inspector;

    /**
     * Wraps {@code element} with the registered default {@link DataInspector}.
     *
     * @param element Jackson node (object or scalar); must not be {@code null}
     */
    public ObjectData(JsonNode element) {
        this(element, DataInspectors.defaultInspector());
    }

    /**
     * Wraps {@code element} with the given session inspector.
     *
     * @param element Jackson node (object or scalar); must not be {@code null}
     * @param inspector backing-store adapter for copy and transform operations
     */
    public ObjectData(JsonNode element, DataInspector inspector) {
        this.element = element;
        this.inspector = inspector;
    }

    /** {@inheritDoc} */
    @Override
    public Data all() {
        if (element.isObject()) {
            return new GroupedData(StreamSupport.stream(spliteratorUnknownSize(element.fields(), 0), false)
                                                .map(Entry::getValue)
                                                .map(node -> new ObjectData(node, inspector))
                                                .map(v -> (Data) v)
                                                .toList());
        } else {
            return this;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        return Mapping.empty();
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
        StreamSupport.stream(spliteratorUnknownSize(element.fields(), 0), false)
                     .map(Entry::getValue)
                     .filter(JsonNode::isObject)
                     .map(node -> new ObjectData(node, inspector))
                     .forEach(action);
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        return json2Value(element.get(fieldName), inspector);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        return element.has(fieldName);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isObject() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode toJson() {
        return element;
    }

    /** {@inheritDoc} */
    @Override
    public JSONataResult toNode() {
        return JSONataResults.object(element);
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Function<JsonNode, Data> function) {
        return function.apply(element);
    }

    /** {@inheritDoc} */
    @Override
    public DataInspector inspector() {
        return inspector;
    }

    @Override
    public String toString() {
        return String.format("Object [%s]", element);
    }
}
