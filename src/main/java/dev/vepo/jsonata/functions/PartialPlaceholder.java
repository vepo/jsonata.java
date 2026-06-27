package dev.vepo.jsonata.functions;

/**
 * Placeholder for partial application ({@code ?}) in function argument lists.
 *
 * <p>Must not be evaluated via {@link Mapping#map}; {@link FunctionApplyService} recognizes
 * {@link PartialPlaceholder} in argument lists and binds call-site values to the
 * corresponding slots in a {@link PartialApplication}.
 */
public record PartialPlaceholder() implements Mapping {

    /** Singleton placeholder instance used in partial application argument lists. */
    public static final PartialPlaceholder INSTANCE = new PartialPlaceholder();

    /**
     * Must not be called — placeholders are resolved only during function application.
     *
     * @throws dev.vepo.jsonata.exception.JSONataException always
     */
    @Override
    public dev.vepo.jsonata.functions.data.Data map(dev.vepo.jsonata.functions.data.Data original,
                                                    dev.vepo.jsonata.functions.data.Data current) {
        throw new dev.vepo.jsonata.exception.JSONataException("Partial placeholder evaluated outside apply context");
    }
}
