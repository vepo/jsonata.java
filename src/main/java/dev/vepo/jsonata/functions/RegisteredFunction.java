package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata call to an externally registered function from {@link EvaluationEnvironment}.
 *
 * <p>Registered functions are supplied by the embedding application (not parsed from
 * the expression). Arguments are passed as unevaluated {@link Mapping} providers so
 * the registered implementation controls evaluation timing via
 * {@link EvaluationEnvironment.MappingCall}.
 *
 * @param name           registered function name
 * @param valueProviders unevaluated argument expressions
 * @param environment    the embedding environment holding the implementation
 */
public record RegisteredFunction(String name, List<Mapping> valueProviders,
                                 EvaluationEnvironment environment)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var impl = environment.functions().get(name);
        if (impl == null) {
            throw new dev.vepo.jsonata.exception.JSONataException("Function not found: " + name);
        }
        return impl.apply(new EvaluationEnvironment.MappingCall(original, current, valueProviders, List.of()));
    }
}
