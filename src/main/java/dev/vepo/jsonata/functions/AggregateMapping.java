package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Object field value produced by an aggregate expression ({@code $sum}, {@code $count}, etc.).
 *
 * <p>Separates per-element extraction ({@link #extractor()}) from the reducing operation
 * ({@link #operation(Data)}) so {@link ObjectBuilder} can group by field name before
 * applying the aggregate across grouped sequences.
 */
public interface AggregateMapping extends Mapping {

    /**
     * Per-element value expression evaluated before aggregation.
     *
     * @return the mapping that extracts a contribution from each array element
     */
    Mapping extractor();

    /**
     * Applies the aggregate function to a grouped sequence of extracted values.
     *
     * @param original grouped extracted values (typically {@link dev.vepo.jsonata.functions.data.GroupedData})
     * @return the aggregated result
     */
    Data operation(Data original);

    /** {@inheritDoc} */
    default Data map(Data original, Data current) {
        return operation(extractor().map(original, current));
    }
}
