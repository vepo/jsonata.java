package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.FunctionValue;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.PartialApplication;
import dev.vepo.jsonata.results.JSONataResults;

/**
 * Domain {@link Data} wrapper for first-class functions and partial applications.
 * <p>
 * Not JSON-serializable: {@link #toJson()} is {@code null} and navigation returns
 * {@link Mapping#empty()}. Immutable after construction; the wrapped callable is
 * either a {@link FunctionValue} or a {@link PartialApplication}.
 */
public final class FunctionData implements Data {

    private final Object callable;

    /**
     * @param value fully resolved user or declared function
     */
    public FunctionData(FunctionValue value) {
        this.callable = value;
    }

    /**
     * @param partial function with some arguments already bound
     */
    public FunctionData(PartialApplication partial) {
        this.callable = partial;
    }

    /**
     * @return {@code true} when the wrapped value is a {@link FunctionValue}
     */
    public boolean isFunctionValue() {
        return callable instanceof FunctionValue;
    }

    /**
     * @return {@code true} when the wrapped value is a {@link PartialApplication}
     */
    public boolean isPartialApplication() {
        return callable instanceof PartialApplication;
    }

    /**
     * @return the wrapped {@link FunctionValue}
     * @throws IllegalStateException when this instance wraps a partial application
     */
    public FunctionValue asFunctionValue() {
        if (callable instanceof FunctionValue fv) {
            return fv;
        }
        throw new IllegalStateException("Not a function value");
    }

    /**
     * @return the wrapped {@link PartialApplication}
     * @throws IllegalStateException when this instance wraps a function value
     */
    public PartialApplication asPartialApplication() {
        if (callable instanceof PartialApplication pa) {
            return pa;
        }
        throw new IllegalStateException("Not a partial application");
    }

    /**
     * @return the underlying callable ({@link FunctionValue} or {@link PartialApplication})
     */
    public Object callable() {
        return callable;
    }

    /** {@inheritDoc} */
    @Override
    public Data all() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Data at(int index) {
        return Mapping.empty();
    }

    /** {@inheritDoc} */
    @Override
    public void forEachChild(Consumer<Data> action) {
    }

    /** {@inheritDoc} */
    @Override
    public Data get(String fieldName) {
        return Mapping.empty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasField(String fieldName) {
        return false;
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

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return false;
    }
}
