package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.arrayNode;
import static dev.vepo.jsonata.expression.transformers.JsonFactory.booleanValue;
import static dev.vepo.jsonata.expression.transformers.JsonFactory.json2Value;
import static dev.vepo.jsonata.expression.transformers.JsonFactory.stringValue;
import static dev.vepo.jsonata.expression.transformers.Value.empty;
import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

@FunctionalInterface
public interface Expression {
    public static class ArrayCastTransformerExpression implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (current.isObject()) {
                return new GroupedValue(singletonList(current));
            } else {
                return current;
            }
        }
    }

    public static record ArrayRangeExpression(int start, int end) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (!current.isArray()) {
                return current;
            }
            if (start < current.lenght()) {
                return new GroupedValue(range(start, min(end + 1, current.lenght())).mapToObj(current::at) 
                                                                                    .toList());
            } else {
                return empty();
            }
        }

    }

    public static record ArrayIndexExpression(int index) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (!current.isArray()) {
                return current;
            }
            if (index >= 0 && index < current.lenght()) {
                return current.at(index);
            } else if (index < 0 && -index < current.lenght()) {
                return current.at(current.lenght() + index);
            } else {
                return empty();
            }
        }

    }

    public static record FieldPathExpression(List<String> fields) implements Expression {

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

    public enum BooleanOperator {
        AND("and"),
        OR("or");

        public static BooleanOperator get(String value) {
            return Stream.of(values())
                         .filter(op -> op.value.compareTo(value) == 0)
                         .findAny()
                         .orElseThrow(() -> new IllegalStateException(String.format("Invalid operator!! operator=%s", value)));
        }

        private String value;

        BooleanOperator(String value) {
            this.value = value;
        }
    }

    public enum CompareOperator {
        EQUAL("="),
        EQUAL_NOT("!="),
        GREATER_THAN(">="),
        GREATER(">"),
        LESS_THAN("<="),
        LESS("<"),
        IN("in");

        public static CompareOperator get(String value) {
            return Stream.of(values())
                         .filter(op -> op.value.compareTo(value) == 0)
                         .findAny()
                         .orElseThrow(() -> new IllegalStateException(String.format("Invalid operator!! operator=%s", value)));
        }

        private String value;

        CompareOperator(String value) {
            this.value = value;
        }
    }

    public static record InnerExpressions(List<Expression> inner) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            return json2Value(inner.stream().reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                            .map(f -> f.map(original, current)
                                                       .toJson())
                                            .orElse(current.toJson()));
        }
    }

    public static record BoleanExpression(BooleanOperator operator, List<Expression> rightExpressions) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            return booleanValue(compare(current.toJson(),
                                rightExpressions.stream()
                                        .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                        .map(f -> f.map(original, original)
                                                .toJson())
                                        .orElse(current.toJson())));
        }

        private boolean compare(JsonNode left, JsonNode right) {
            return switch (operator) {
                case AND -> left.asBoolean() && right.asBoolean();
                case OR -> left.asBoolean() || right.asBoolean();
            };
        }

    }

    public static record CompareExpression(CompareOperator operator, List<Expression> rightExpressions) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            return booleanValue(compare(current.toJson(),
                                rightExpressions.stream()
                                                .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                                .map(f -> f.map(original, original)
                                                           .toJson())
                                                .orElse(current.toJson())));
        }

        private boolean compare(JsonNode left, JsonNode right) {
            return switch (operator) {
                case EQUAL -> left.equals(right);
                case EQUAL_NOT -> !left.equals(right);
                case GREATER -> left.asInt() > right.asInt();
                case GREATER_THAN -> left.asInt() >= right.asInt();
                case LESS -> left.asInt() < right.asInt();
                case LESS_THAN -> left.asInt() <= right.asInt();
                case IN -> right.isArray() && StreamSupport.stream(spliteratorUnknownSize(right.elements(), 0), false)
                        .anyMatch(el -> el.equals(left));
            };
        }
    }

    public static record ArrayConstructorExpression(List<Function<Value, Value>> arrayBuilder) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            if (current.isArray() && arrayBuilder.size() == 1) {
                var elements = new ArrayList<Value>();
                for (int i=0; i < current.lenght();++i) {
                    elements.add(arrayBuilder.get(0).apply(current.at(i)));
                }
                return json2Value(new GroupedValue(elements).toJson());
            } else {
                return json2Value(arrayNode(arrayBuilder.stream().map(fn -> fn.apply(current).toJson()).toList()));
            }
        }
    }

    public static record StringConcatExpression(List<Function<Value, Value>> sources) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            return stringValue(sources.stream()
                    .map(fn -> fn.apply(current).toJson().asText())
                    .collect(joining()));
        }
    }

    public static record FieldPredicateExpression(String fieldName, String content) implements Expression {

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

    public static record DeepFindByFieldNameExpression(String fieldName) implements Expression {

        @Override
        public Value map(Value original, Value current) {
            var availableNodes = new LinkedList<Value>();
            var matchedNodes = new ArrayList<Value>();
            availableNodes.add(current);
            while (!availableNodes.isEmpty()) {
                var currNode = availableNodes.pollFirst();
                if (currNode.isEmpty()) {                    
                    continue;
                }

                if (currNode.isObject() && currNode.hasField(fieldName)) {
                    matchedNodes.add(currNode);                       
                } else {
                    currNode.forEachChild(availableNodes::offerLast);
                }
            }
            if (!matchedNodes.isEmpty()) {
                return new GroupedValue(matchedNodes).get(fieldName);
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

    Value map(Value original, Value current);
}
