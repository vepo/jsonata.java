package dev.vepo.jsonata.functions;

/**
 * A single field definition within {@link ObjectBuilder} or {@link ObjectMapper}.
 *
 * @param name  expression producing the field name
 * @param value expression producing the field value
 * @param merge when {@code true}, merge this field into existing object keys rather than replace
 */
public record FieldContent(Mapping name, Mapping value, boolean merge) {
}
