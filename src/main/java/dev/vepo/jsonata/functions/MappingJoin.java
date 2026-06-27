package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.jsonata.functions.data.ArrayData;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata path join ({@code expr1 ~> expr2} or implicit composition after navigation).
 *
 * <p>Evaluates {@code first} to obtain a focus value, then applies {@code second} with
 * appropriate context propagation:
 * <ul>
 *   <li>Array results are mapped element-wise with parent binding unless the right side
 *       is an {@link ArrayConstructor} (preserving array shape).</li>
 *   <li>{@link PositionalBind} and {@link ContextBind} on the left operand install
 *       index or focus variables for the right operand.</li>
 * </ul>
 *
 * @param first  the left-hand path operand
 * @param second the right-hand path operand
 */
public record MappingJoin(Mapping first, Mapping second) implements Mapping {
    private static final Logger logger = LoggerFactory.getLogger(MappingJoin.class);

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        logger.atDebug().log("MappingJoin: first={} second={} current={}", first, second, current);
        var left = unwrapBind(first);
        var value = left.mapping().map(original, current);
        Data result;
        if (left.indexVariable() != null) {
            result = mapWithIndexBinding(original, current, value, left.indexVariable());
        } else if (left.focusVariable() != null) {
            result = mapWithFocusBinding(original, current, value, left.focusVariable());
        } else if ((value.isArray() || value.isList()) && !(second instanceof ArrayConstructor)) {
            var mapped = value.stream()
                                .map(v -> mapWithParent(v, original, v))
                                .flatMap(Data::stream)
                                .filter(Predicate.not(Data::isEmpty))
                                .toList();
            if (ChainedMapping.terminalObjectMapper(second) != null) {
                result = new ArrayData(JsonFactory.arrayNode(mapped.stream().map(Data::toJson).toList()));
            } else {
                result = new GroupedData(mapped);
            }
        } else {
            result = mapWithParent(value, original, value);
        }
        logger.atDebug().log("MappingJoin: result={}", result);
        return result;
    }

    private Data mapWithIndexBinding(Data original, Data current, Data value, String indexVariable) {
        if (!value.isArray() && !value.isList()) {
            PathBindings.bindIndex(indexVariable, 0);
            try {
                return mapWithParent(value, original, value);
            } finally {
                PathBindings.removeBinding(indexVariable);
            }
        }
        var results = new ArrayList<Data>();
        for (int i = 0; i < value.length(); i++) {
            PathBindings.bindIndex(indexVariable, i);
            try {
                var item = value.at(i);
                results.addAll(mapWithParent(item, original, item).stream().toList());
            } finally {
                PathBindings.removeBinding(indexVariable);
            }
        }
        return new GroupedData(results);
    }

    private Data mapWithFocusBinding(Data original, Data focusContext, Data value, String focusVariable) {
        if (!value.isArray() && !value.isList()) {
            PathBindings.bind(focusVariable, value);
            try {
                return second.map(original, focusContext);
            } finally {
                PathBindings.removeBinding(focusVariable);
            }
        }
        var results = new ArrayList<Data>();
        for (int i = 0; i < value.length(); i++) {
            var item = value.at(i);
            PathBindings.bind(focusVariable, item);
            try {
                results.addAll(second.map(original, focusContext).stream().toList());
            } finally {
                PathBindings.removeBinding(focusVariable);
            }
        }
        return new GroupedData(results);
    }

    private Data mapWithParent(Data parent, Data original, Data current) {
        PathBindings.pushParent(parent);
        try {
            return second.map(original, current);
        } finally {
            PathBindings.popParent();
        }
    }

    private static BindInfo unwrapBind(Mapping mapping) {
        if (mapping instanceof PositionalBind positional) {
            return new BindInfo(positional.operand(), positional.variableName(), null);
        }
        if (mapping instanceof ContextBind context) {
            return new BindInfo(context.operand(), null, context.variableName());
        }
        return new BindInfo(mapping, null, null);
    }

    private record BindInfo(Mapping mapping, String indexVariable, String focusVariable) {
    }
}
