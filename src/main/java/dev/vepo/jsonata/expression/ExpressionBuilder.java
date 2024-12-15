package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.ValueFactory.empty;
import static dev.vepo.jsonata.expression.transformers.ValueFactory.json2Value;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.IntStream;

import dev.vepo.jsonata.expression.generated.ExpressionsBaseListener;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.FieldNameContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.IndexPredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.InnerExpressionContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.QueryPathContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.RangePredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.RootPathContext;
import dev.vepo.jsonata.expression.transformers.GroupedValue;

public class ExpressionBuilder extends ExpressionsBaseListener {
    private static String fieldName2Text(FieldNameContext ctx) {
        if (Objects.nonNull(ctx.IDENTIFIER())) {
            return ctx.getText();
        } else {
            return ctx.getText().substring(1, ctx.getText().length() - 1);
        }
    }

    private final Queue<List<Expression>> expressions;

    public ExpressionBuilder() {
        this.expressions = new LinkedList<>();
        this.expressions.add(new ArrayList<>()); // root
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        expressions.peek().add((original, value) -> original);
    }

    @Override
    public void exitQueryPath(QueryPathContext ctx) {
        var fieldNames = ctx.fieldName().stream().map(ExpressionBuilder::fieldName2Text).toList();
        expressions.peek()
                   .add((original, value) -> {
                       var currNode = value;
                       for (var field : fieldNames) {
                           if (currNode.isEmpty()) {
                               break;
                           } else if (currNode.hasField(field)) {
                               currNode = currNode.get(field);
                           } else {
                               currNode = empty();
                           }
                       }
                       return currNode;
                   });
    }

    @Override
    public void enterInnerExpression(InnerExpressionContext ctx) {
        this.expressions.add(new ArrayList<>()); // new stack
    }

    @Override
    public void exitInnerExpression(InnerExpressionContext ctx) {
        var innerExpressions = this.expressions.poll();
        this.expressions.peek()
                        .add((original, value) -> json2Value(innerExpressions.stream()
                                                                             .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                                                             .get()
                                                                             .map(original, original)
                                                                             .toJson()));
    }

    @Override
    public void exitRangePredicateArray(RangePredicateArrayContext ctx) {
        var startIndex = Integer.valueOf(ctx.rangePredicate().NUMBER(0).getText());
        var endIndex = Integer.valueOf(ctx.rangePredicate().NUMBER(1).getText());
        if (startIndex < 0) {
            throw new InvalidParameterException("Start index should be greather than 0!");
        }
        if (endIndex < 0) {
            throw new InvalidParameterException("End index should be greather than 0!");
        }
        if (endIndex < startIndex) {
            throw new InvalidParameterException("End index should be greather than start index!");
        }
        expressions.peek()
                   .add((original, value) -> {
                       if (!value.isArray()) {
                           return value;
                       }
                       if (startIndex < value.lenght()) {
                           return new GroupedValue(IntStream.range(startIndex, Math.min(endIndex + 1, value.lenght()))
                                                            .mapToObj(value::at)
                                                            .toList());
                       } else {
                           return empty();
                       }
                   });
    }

    @Override
    public void exitIndexPredicateArray(IndexPredicateArrayContext ctx) {
        var index = Integer.valueOf(ctx.indexPredicate().NUMBER().getText());
        expressions.peek()
                   .add((original, value) -> {
                       if (!value.isArray()) {
                           return value;
                       }
                       if (index >= 0 && index < value.lenght()) {
                           return value.at(index);
                       } else if (index < 0 && -index < value.lenght()) {
                           return value.at(value.lenght() + index);
                       } else {
                           return empty();
                       }
                   });
    }

    public List<Expression> getExpressions() {
        return expressions.peek();
    }
}