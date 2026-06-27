package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;

import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.signature.FunctionSignature;

/**
 * Parsed user-defined function with lexical closure and optional type signature.
 *
 * <p>Combines parameter names, enclosing {@link BlockContext} (closure), body
 * {@link Mapping}, and an optional {@link FunctionSignature} for argument validation.
 * Invocation merges closure variables, parameters, and current focus before evaluating
 * the body.
 *
 * @param parameterNames  positional parameter identifiers
 * @param closureContext  lexical scope where the function was declared
 * @param body            the function body expression or block
 * @param signature       optional argument type signature from the parser
 */
public record DeclaredFunction(List<String> parameterNames, BlockContext closureContext, Mapping body,
                               Optional<FunctionSignature> signature) {

    /**
     * Creates a function without an explicit signature.
     *
     * @param parameterNames positional parameter identifiers
     * @param closureContext lexical scope where the function was declared
     * @param body           the function body
     */
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

    /**
     * Wraps this declaration as a first-class {@link FunctionValue} without captured context.
     *
     * @return a function value ready for application or wrapping
     */
    public FunctionValue asValue() {
        return new FunctionValue(this, Optional.empty(), signature);
    }

    /**
     * Wraps this declaration with an explicit signature override.
     *
     * @param sig the signature to attach
     * @return a function value with the given signature
     */
    public FunctionValue asValue(Optional<FunctionSignature> sig) {
        return new FunctionValue(this, Optional.empty(), sig);
    }

    /**
     * Invokes the function body with merged closure, parameter, and focus context.
     *
     * @param original        root input document
     * @param current         current focus (may include parameter overlay)
     * @param invocationFrame frame holding parameter bindings for this call
     * @return the body result
     */
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
