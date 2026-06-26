package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.MappingParser;
import dev.vepo.jsonata.functions.data.Data;

public record Eval(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var expr = BuiltInArgs.evaluateOne(providers, original, current).toJson().asText();
        var mappings = MappingParser.parse(expr);
        if (mappings.isEmpty()) {
            return Mapping.empty();
        }
        return mappings.get(0).map(original, current);
    }
}
