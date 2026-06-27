package dev.vepo.jsonata.functions;

import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata object transform ({@code object{field: expr, ...}} applied to input).
 *
 * <p>Unlike {@link ObjectBuilder} (literal construction), {@code ObjectMapper} maps over
 * the current focus: for an object input it produces one transformed object; for an array
 * input it produces an array of transformed objects with optional field merge semantics.
 *
 * @param contents field name/value (and merge flag) definitions
 */
public record ObjectMapper(List<FieldContent> contents) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            var builder = JsonFactory.objectBuilder();
            contents.forEach(content -> builder.set(content.name().map(original, current).toJson().asText(),
                                                    content.value().map(original, current)));
            return builder.build();
        } else if (current.isArray() || current.isList()) {
            var newContents = new ArrayList<JsonNode>();
            range(0, current.length()).forEach(i -> {
                var item = current.at(i);
                var builder = JsonFactory.objectBuilder();
                contents.forEach(content -> builder.set(content.name().map(original, item).toJson().asText(),
                                                        content.value().map(original, item),
                                                        content.merge()));
                newContents.add(builder.root());
            });
            return new ArrayData(JsonFactory.arrayNode(newContents));
        } else {
            return Mapping.empty();
        }
    }
}
