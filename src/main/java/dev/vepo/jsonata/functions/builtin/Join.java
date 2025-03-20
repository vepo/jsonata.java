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

    public Join {
        if (providers.size() < 1 || providers.size() > 2) {
            throw new IllegalArgumentException("$join function must have 1 or 2 arguments!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var data = providers.get(0).map(original, current);
        if (data.isArray() || data.isList()) {
            return JsonFactory.stringValue(data.stream()
                                               .map(v -> v.toJson()
                                                          .asText())
                                               .collect(joinString(original, current)));
        } else {
            return data;
        }
    }

    private Collector<CharSequence, ? extends Object, String> joinString(Data original, Data current) {
        return providers.size() == 2 ? joining(providers.get(1)
                                                        .map(original, current)
                                                        .toJson()
                                                        .asText())
                                     : joining();
    }
}