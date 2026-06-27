package dev.vepo.jsonata.functions;

import static java.util.Collections.singletonList;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata array cast: wraps a singleton object in a one-element sequence.
 *
 * <p>When {@code current} is an object, returns a grouped sequence containing that object;
 * otherwise passes {@code current} through unchanged. Used where the grammar requires
 * sequence semantics for object input.
 */
public class ArrayCast implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            return new GroupedData(singletonList(current));
        } else {
            return current;
        }
    }
}
