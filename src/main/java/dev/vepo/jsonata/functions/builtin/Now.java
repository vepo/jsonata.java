package dev.vepo.jsonata.functions.builtin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $now}. Returns the current timestamp as an ISO 8601 string.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Now(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                                           .withZone(ZoneOffset.UTC);

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (providers.size() == 1) {
            var picture = providers.get(0).map(original, current).toJson().asText();
            return JsonFactory.stringValue(ISO_FORMAT.format(Instant.now()));
        }
        return JsonFactory.stringValue(ISO_FORMAT.format(Instant.now()));
    }
}
