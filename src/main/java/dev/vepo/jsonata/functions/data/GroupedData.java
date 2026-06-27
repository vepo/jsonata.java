package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.arrayNode;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.results.JSONataResults;

/**
 * Immutable sequence of {@link Data} values produced by multi-match navigation
 * (wildcard projection, array mapping, predicate filtering).
 * <p>
 * In JSONata terms this is a <em>group</em> of coerced results, not a JSON array
 * value in the input document. {@link #isList()} is {@code true}; {@link #isArray()}
 * is {@code false}. Empty elements are dropped during field projection.
 */
public class GroupedData implements Data {

    private final List<Data> elements;

    /**
     * @param elements constitutent matches; list is stored by reference and must not be mutated afterward
     */
    public GroupedData(List<Data> elements) {
        this.elements = elements;
    }

    /** {@inheritDoc} */
    @Override
    public Data all() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        return elements.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
        elements.forEach(action);
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        return new GroupedData(elements.stream()
                                        .filter(v -> v.hasField(fieldName))
                                        .map(e -> e.get(fieldName))
                                        .filter(v -> !v.isEmpty())
                                        .toList());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        return elements.stream().anyMatch(e -> e.hasField(fieldName));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isArray() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isList() {
        return true;
    }    

    /** {@inheritDoc} */
    @Override
    public int length() {
        return elements.size();
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode toJson() {
        return arrayNode(elements.stream()
                .map(Data::toJson)
                .toList());
    }

    /** {@inheritDoc} */
    @Override
    public JSONataResult toNode() {
        return JSONataResults.group(elements.stream()
                                            .map(Data::toNode)
                                            .toList());
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Function<JsonNode, Data> function) {
        return new GroupedData(elements.stream()
                                      .map(e -> e.map(function))
                                      .toList());
    }

    /** {@inheritDoc} */
    @Override
    public Stream<Data> stream() {
        return elements.stream();
    }
    
    @Override
    public String toString() {
        return String.format("Grouped [%s]", elements);
    }
}
