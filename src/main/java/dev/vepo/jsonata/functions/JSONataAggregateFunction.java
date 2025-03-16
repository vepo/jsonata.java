package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public interface JSONataAggregateFunction extends JSONataFunction {
    JSONataFunction extractor();
    Data operation(Data original);
    
    default Data map(Data original, Data current) {
        return operation(extractor().map(original, current));
    }
}
