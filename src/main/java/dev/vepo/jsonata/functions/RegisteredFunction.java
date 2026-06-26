package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.functions.data.Data;

public record RegisteredFunction(String name, List<Mapping> valueProviders,
                                 EvaluationEnvironment environment)
        implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var impl = environment.functions().get(name);
        if (impl == null) {
            throw new dev.vepo.jsonata.exception.JSONataException("Function not found: " + name);
        }
        return impl.apply(new EvaluationEnvironment.MappingCall(original, current, valueProviders, List.of()));
    }
}
