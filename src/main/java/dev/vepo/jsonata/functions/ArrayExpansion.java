package dev.vepo.jsonata.functions;

import java.util.stream.IntStream;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ArrayExpansion(Mapping left, Mapping right) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var leftValue = left.map(original, current);
        var rightValue = right.map(original, current);
        if (!leftValue.isObject() || !leftValue.toJson().isInt()) {
            throw new JSONataException("Left value is not a number! value=" + leftValue);
        }

        if (!rightValue.isObject() || !rightValue.toJson().isInt()) {
            throw new JSONataException("Left value is not a number! value=" + rightValue);
        }

        return JsonFactory.arrayValue(IntStream.rangeClosed(leftValue.toJson()
                                                                     .intValue(),
                                                            rightValue.toJson()
                                                                      .intValue())
                                               .toArray());
    }

}
