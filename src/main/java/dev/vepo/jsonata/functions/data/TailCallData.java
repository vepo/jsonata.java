package dev.vepo.jsonata.functions.data;

import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.TailCallThunk;
import dev.vepo.jsonata.functions.FunctionApplyService;
import dev.vepo.jsonata.results.JSONataResults;

/**
 * {@link Data} wrapper for tail-call thunks in the TCO trampoline.
 */
public final class TailCallData implements Data {

    private final TailCallThunk thunk;

    public TailCallData(TailCallThunk thunk) {
        this.thunk = thunk;
    }

    public Data execute() {
        return FunctionApplyService.resolveTailCall(thunk);
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
