package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public interface AggregateMapping extends Mapping {
    Mapping extractor();
    Data operation(Data original);
    
    default Data map(Data original, Data current) {
        return operation(extractor().map(original, current));
    }
}
