package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

public record DeclaredFunction(List<String> parameterNames, BlockContext context, Mapping functions) {

    public Data accept(Data original, Data current, BlockContext context) {
        var builder = objectBuilder();
        builder.fill(current);
        builder.fill(context.variables(original, current));
        return functions.map(original, builder.build());
    }
}
