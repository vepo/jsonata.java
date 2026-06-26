package dev.vepo.jsonata.functions.builtin;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Collector;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Join(List<Mapping> providers,
                   List<DeclaredFunction> declaredFunctions)
        implements Mapping {

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
