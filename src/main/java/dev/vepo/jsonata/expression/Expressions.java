package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonValue.empty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.JSONata;
import dev.vepo.jsonata.expression.generated.ExpressionsBaseListener;
import dev.vepo.jsonata.expression.generated.ExpressionsLexer;
import dev.vepo.jsonata.expression.generated.ExpressionsParser;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.FieldNameContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.InnerExpressionContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.PredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.QueryPathContext;
import dev.vepo.jsonata.expression.transformers.JsonValue;

public class Expressions {

    public static class ExpressionBuilder extends ExpressionsBaseListener {
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
                            .add((original, value) -> JsonValue.toValue(innerExpressions.stream()
                                                                                        .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                                                                                        .get()
                                                                                        .map(original, original)
                                                                                        .toJson()));
        }

        @Override
        public void exitPredicateArray(PredicateArrayContext ctx) {
            var index = Integer.valueOf(ctx.predicate().NUMBER().getText());
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

    public static JSONata parse(String content) {
        var parser = new ExpressionsParser(new CommonTokenStream(new ExpressionsLexer(CharStreams.fromString(content))));
        var walker = new ParseTreeWalker();

        var builder = new ExpressionBuilder();
        walker.walk(builder, parser.expressions());
        return new JSONata(builder.getExpressions());
    }

}
