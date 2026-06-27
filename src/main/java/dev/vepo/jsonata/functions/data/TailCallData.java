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
 * Domain {@link Data} wrapper for deferred tail-call thunks in the TCO trampoline.
 * <p>
 * Holds a {@link TailCallThunk} until {@link #execute()} resolves it via
 * {@link FunctionApplyService}. Not JSON-serializable; navigation returns
 * {@link Mapping#empty()}. Immutable after construction.
 */
public final class TailCallData implements Data {

    private final TailCallThunk thunk;

    /**
     * @param thunk deferred tail call to resolve when the trampoline runs
     */
    public TailCallData(TailCallThunk thunk) {
        this.thunk = thunk;
    }

    /**
     * Resolves this thunk through the tail-call optimizer.
     *
     * @return result of applying the deferred function call
     */
    public Data execute() {
        return FunctionApplyService.resolveTailCall(thunk);
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
