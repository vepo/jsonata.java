package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Mutable variable binding cell supporting forward references in blocks.
 */
final class VariableCell implements Mapping {

    private Mapping binding = (o, c) -> Mapping.empty();

    void set(Mapping binding) {
        this.binding = binding;
    }

    @Override
    public Data map(Data original, Data current) {
        return binding.map(original, current);
    }
}
