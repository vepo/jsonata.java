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
 * {@link Data} wrapper for first-class functions and partial applications.
 */
public final class FunctionData implements Data {

    private final Object callable;

    public FunctionData(FunctionValue value) {
        this.callable = value;
    }

    public FunctionData(PartialApplication partial) {
        this.callable = partial;
    }

    public boolean isFunctionValue() {
        return callable instanceof FunctionValue;
    }

    public boolean isPartialApplication() {
        return callable instanceof PartialApplication;
    }

    public FunctionValue asFunctionValue() {
        if (callable instanceof FunctionValue fv) {
            return fv;
        }
        throw new IllegalStateException("Not a function value");
    }

    public PartialApplication asPartialApplication() {
        if (callable instanceof PartialApplication pa) {
            return pa;
        }
        throw new IllegalStateException("Not a partial application");
    }

    public Object callable() {
        return callable;
    }

    @Override
    public Data all() {
        return this;
    }

    @Override
    public Data at(int index) {
        return Mapping.empty();
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
    }

    @Override
    public Data get(String fieldName) {
        return Mapping.empty();
    }

    @Override
    public boolean hasField(String fieldName) {
        return false;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public JsonNode toJson() {
        return null;
    }

    @Override
    public JSONataResult toNode() {
        return JSONataResults.empty();
    }

    @Override
    public Data map(Function<JsonNode, Data> function) {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
