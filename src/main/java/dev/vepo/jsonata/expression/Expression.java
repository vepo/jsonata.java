package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.vepo.jsonata.expression.transformers.JsonFactory;
import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

@FunctionalInterface
public interface Expression {
    Value map(Value original, Value current);

    public static class ArrayCastTransformerExpression implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (!current.isEmpty() && !current.isArray() && current.lenght() == 1) {
                return new GroupedValue(Collections.singletonList(current));
            } else {
                return current;
            }
        }
    }

    public static class FieldPathExpression implements Expression {
        private final List<String> fields;

        public FieldPathExpression(List<String> fields) {
            this.fields = fields;
        }

        @Override
        public Value map(Value original, Value current) {
            var currNode = current;
            for (var field : fields) {
                if (currNode.isEmpty()) {
                    break;
                } else if (currNode.hasField(field)) {
                    currNode = currNode.get(field);
                } else {
                    currNode = empty();
                }
            }
            return currNode;
        }

    }

    public static class StringConcatExpression implements Expression {
        private final List<Function<Value, Value>> sources;

        public StringConcatExpression(List<Function<Value, Value>> sources) {
            this.sources = sources;
        }

        @Override
        public Value map(Value original, Value current) {
            return JsonFactory.stringValue(sources.stream().map(fn -> fn.apply(current).toJson().asText()).collect(Collectors.joining()));
        }
    }

    public static class FieldPredicateExpression implements Expression {

        private final String fieldName;
        private final String content;

        public FieldPredicateExpression(String fieldName, String content) {
            this.fieldName = fieldName;
            this.content = content;
        }

        @Override
        public Value map(Value original, Value current) {
            if (!current.isArray()) {
                return current;
            }
            if (current.hasField(fieldName)) {
                var matched = new ArrayList<Value>();
                for (int i = 0; i < current.lenght(); ++i) {
                    var inner = current.at(i);
                    var innerContent = inner.get(fieldName).toJson();
                    if (innerContent.asText().equals(content)) {
                        matched.add(inner);
                    }
                }
                return new GroupedValue(matched);
            } else {
                return empty();
            }
        }

    }

    public static class WildcardExpression implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (!current.isEmpty() && !current.isArray() && current.lenght() == 1) {
                return current.all();
            } else {
                return current;
            }
        }

    }
}
