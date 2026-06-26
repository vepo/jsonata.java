package dev.vepo.jsonata.functions.builtin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record FromMillis(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                                           .withZone(ZoneOffset.UTC);

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        var millis = arg.toJson().asLong();
        return JsonFactory.stringValue(ISO_FORMAT.format(Instant.ofEpochMilli(millis)));
    }
}
