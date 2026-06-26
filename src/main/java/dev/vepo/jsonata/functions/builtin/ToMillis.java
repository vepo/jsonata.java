package dev.vepo.jsonata.functions.builtin;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ToMillis(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final DateTimeFormatter ISO_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
            .optionalEnd()
            .optionalStart()
            .appendPattern("X")
            .optionalEnd()
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 2, false, original, current);
        var timestamp = args.get(0).toJson().asText();
        try {
            var instant = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
            return JsonFactory.numberValue(instant.toEpochMilli());
        } catch (Exception e) {
            try {
                var instant = Instant.from(ISO_PARSER.parse(timestamp));
                return JsonFactory.numberValue(instant.toEpochMilli());
            } catch (Exception e2) {
                return Mapping.empty();
            }
        }
    }
}
