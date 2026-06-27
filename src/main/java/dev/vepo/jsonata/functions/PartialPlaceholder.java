package dev.vepo.jsonata.functions;

/**
 * Placeholder for partial application {@code ?} in function argument lists.
 */
public record PartialPlaceholder() implements Mapping {

    public static final PartialPlaceholder INSTANCE = new PartialPlaceholder();

    @Override
    public dev.vepo.jsonata.functions.data.Data map(dev.vepo.jsonata.functions.data.Data original,
                                                    dev.vepo.jsonata.functions.data.Data current) {
        throw new dev.vepo.jsonata.exception.JSONataException("Partial placeholder evaluated outside apply context");
    }
}
