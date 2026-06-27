package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.regex.RegExp;

/**
 * Domain {@link Data} for a JSONata regular-expression literal.
 * <p>
 * Only {@link #asRegex()} and {@link #isRegex()} are supported; general navigation
 * throws {@link UnsupportedOperationException}. The pattern text is held as a Jackson
 * text node; {@link RegExp#compile(String)} provides a shared, thread-safe cache.
 */
public class RegexData implements Data {

    private final JsonNode node;

    /**
     * @param node text node containing the regex literal source (including delimiters/flags)
     */
    public RegexData(JsonNode node) {
        this.node = node;
    }

    /** {@inheritDoc} */
    @Override
    public Data all() {
        throw new UnsupportedOperationException("Unimplemented method 'all'");
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'at'");
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
        throw new UnsupportedOperationException("Unimplemented method 'forEachChild'");
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        throw new UnsupportedOperationException("Unimplemented method 'hasField'");
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        throw new UnsupportedOperationException("Unimplemented method 'length'");
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode toJson() {
        throw new UnsupportedOperationException("Unimplemented method 'toJson'");
    }

    /** {@inheritDoc} */
    @Override
    public JSONataResult toNode() {
        throw new UnsupportedOperationException("Unimplemented method 'toNode'");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRegex() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return compiled {@link RegExp} from the global pattern cache
     * @throws IllegalArgumentException if the pattern text is not a valid JavaScript regex
     */
    @Override
    public RegExp asRegex() {
        return RegExp.compile(node.asText());
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Function<JsonNode, Data> function) {
        throw new UnsupportedOperationException("Unimplemented method 'map'");
    }

    @Override
    public String toString() {
        return String.format("RegexData [%s]", node);
    }
}
