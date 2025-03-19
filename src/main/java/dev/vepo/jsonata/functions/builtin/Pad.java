package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Pad(List<Mapping> providers,
                  List<DeclaredFunction> declaredFunctions)
        implements Mapping {
    public Pad {
        if (providers.size() < 2 || providers.size() > 3) {
            throw new IllegalArgumentException("$pad function must have 2 or 3 arguments");
        }
    }

    @Override
    public Data map(Data original, Data current) {
        var padLength = providers.get(1).map(original, current).toJson().asInt();
        if (providers.size() == 2) {
            if (padLength > 0) {
                return JsonFactory.stringValue(StringUtils.rightPad(providers.get(0)
                                                                             .map(original, current)
                                                                             .toJson()
                                                                             .asText(),
                                                                    padLength));
            } else if (padLength < 0) {
                return JsonFactory.stringValue(StringUtils.leftPad(providers.get(0)
                                                                            .map(original, current)
                                                                            .toJson()
                                                                            .asText(),
                                                                   Math.abs(padLength)));
            } else {
                return JsonFactory.stringValue(providers.get(0)
                                                        .map(original, current)
                                                        .toJson()
                                                        .asText());
            }
        } else {
            var content = providers.get(2).map(original, current).toJson().asText();
            if (padLength > 0) {
                return JsonFactory.stringValue(StringUtils.rightPad(providers.get(0)
                                                                             .map(original, current)
                                                                             .toJson()
                                                                             .asText(),
                                                                    padLength,
                                                                    content));
            } else if (padLength < 0) {
                return JsonFactory.stringValue(StringUtils.leftPad(providers.get(0)
                                                                            .map(original, current)
                                                                            .toJson()
                                                                            .asText(),
                                                                   Math.abs(padLength),
                                                                   content));
            } else {
                return JsonFactory.stringValue(providers.get(0)
                                                        .map(original, current)
                                                        .toJson()
                                                        .asText());
            }
        }

    }
}
