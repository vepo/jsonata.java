package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;

import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * Parsed user-defined function with lexical closure and optional signature.
 */
public record DeclaredFunction(List<String> parameterNames, BlockContext closureContext, Mapping body,
                               Optional<FunctionSignature> signature) {

    public DeclaredFunction(List<String> parameterNames, BlockContext closureContext, Mapping body) {
        this(parameterNames, closureContext, body, Optional.empty());
    }

    /** @deprecated use {@link #body()} */
    public Mapping functions() {
        return body;
    }

    /** @deprecated use {@link #closureContext()} */
    public BlockContext context() {
        return closureContext;
    }

    public FunctionValue asValue() {
        return new FunctionValue(this, Optional.empty(), signature);
    }

    public FunctionValue asValue(Optional<FunctionSignature> sig) {
        return new FunctionValue(this, Optional.empty(), sig);
    }

    public Data accept(Data original, Data current, BlockContext invocationFrame) {
        var builder = objectBuilder();
        builder.fill(current);
        builder.fill(invocationFrame.variables(original, current));
        builder.fill(closureContext.variables(original, current));
        var effective = builder.build();
        if (body instanceof TailCallMapping tailCall) {
            return tailCall.mapWithFrame(original, effective, invocationFrame);
        }
        return body.map(original, effective);
    }
}
