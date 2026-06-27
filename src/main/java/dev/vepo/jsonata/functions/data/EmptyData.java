package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.results.JSONataResults;

/**
 * Sentinel for the JSONata <em>empty sequence</em> — no match, not JSON {@code null}.
 * <p>
 * Immutable singleton-like behavior: navigation returns {@code this}, {@link #isEmpty()}
 * is {@code true}, and {@link #toJson()} is {@code null}. Used by
 * {@link dev.vepo.jsonata.functions.Mapping#empty()} and as the result of failed or absent navigation.
 *
 * @see Data#isEmpty()
 */
public class EmptyData implements Data {

    /** {@inheritDoc} */
    @Override
    public Data all() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
        // nothing! This is empty
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public JsonNode toJson() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public JSONataResult toNode() {
        return JSONataResults.empty();
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Function<JsonNode, Data> function) {
        return this;
    }

    @Override
    public String toString() {
        return "Empty []";
    }
}
