package dev.vepo.jsonata.functions.builtin;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $split}. Splits a string by a separator or regex into an array of strings.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Split(List<Mapping> providers,
                    List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 3, false, original, current);
        var value = args.get(0).toJson().asText();
        var patternData = args.get(1);
        Supplier<String[]> splitFunction = patternData.isRegex() ? () -> patternData.asRegex().split(value)
                                                                 : () -> value.split(patternData.toJson().asText());

        if (args.size() == 3) {
            var limit = args.get(2).toJson().asInt();
            return JsonFactory.arrayValue(Stream.of(splitFunction.get())
                                                .limit(limit)
                                                .toArray(String[]::new));
        } else {
            return JsonFactory.arrayValue(splitFunction.get());
        }
    }
}
