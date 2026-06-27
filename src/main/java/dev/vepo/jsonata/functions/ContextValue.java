package dev.vepo.jsonata.functions;

import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata context value expression ({@code $} or explicit context reference).
 *
 * <p>Evaluates the inner mapping and normalizes the result to a scalar {@link Data}
 * value via {@link JsonFactory#json2Value}, returning empty when the inner result is
 * absent.
 *
 * @param inner the expression whose value represents the context
 */
public record ContextValue(Mapping inner) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        return Optional.ofNullable(inner.map(original, current))
                       .map(Data::toJson)
                       .map(JsonFactory::json2Value)
                       .orElseGet(Mapping::empty);
    }
}
