package dev.vepo.jsonata.functions.buildin;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record SplitJSONataFunction(List<JSONataFunction> valueProviders) implements JSONataFunction {
    public SplitJSONataFunction {
        if (valueProviders.size() < 2 || valueProviders.size() > 3) {
            throw new IllegalArgumentException("$split function must have 2 or 3 arguments!");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var value = valueProviders.get(0).map(original, current).toJson().asText();
        var patternData = valueProviders.get(1).map(original, current);
        Supplier<String[]> splitFunction = patternData.isRegex() ? () -> patternData.asRegex().split(value)
                                                                 : () -> value.split(patternData.toJson().asText());

        if (valueProviders.size() == 3) {
            var limit = valueProviders.get(2).map(original, current).toJson().asInt();
            return JsonFactory.arrayValue(Stream.of(splitFunction.get())
                                                .limit(limit)
                                                .toArray(String[]::new));
        } else {
            return JsonFactory.arrayValue(splitFunction.get());
        }
    }

}
