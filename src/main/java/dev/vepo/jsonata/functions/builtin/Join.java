package dev.vepo.jsonata.functions.builtin;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Collector;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $join}. Joins array elements into a string with an optional separator. Uses context as the array when no arguments are supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Join(List<Mapping> providers,
                   List<DeclaredFunction> declaredFunctions)
        implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 0, 2, true, original, current);
        var data = args.get(0);
        if (data.isArray() || data.isList()) {
            return JsonFactory.stringValue(data.stream()
                                               .map(v -> v.toJson().asText())
                                               .collect(joinString(args, original, current)));
        } else {
            return data;
        }
    }

    private Collector<CharSequence, ?, String> joinString(List<Data> args, Data original, Data current) {
        return args.size() == 2 ? joining(args.get(1).toJson().asText()) : joining();
    }
}
