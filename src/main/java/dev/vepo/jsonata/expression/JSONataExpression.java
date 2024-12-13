package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.Node.emptyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.JSONata;
import dev.vepo.jsonata.JSONata.Expression;
import dev.vepo.jsonata.expression.generated.JSONataQueriesBaseListener;
import dev.vepo.jsonata.expression.generated.JSONataQueriesLexer;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser.ArrayQueryContext;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser.FieldNameContext;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser.FieldQueryContext;

public class JSONataExpression {

    private static class ExpressionBuilder extends JSONataQueriesBaseListener {
        private List<Expression> expressions;

        ExpressionBuilder() {
            expressions = new ArrayList<>();
        }

        private static String fieldName2Text(FieldNameContext ctx) {
            if (Objects.nonNull(ctx.IDENTIFIER())) {
                return ctx.getText();
            } else {
                return ctx.getText().substring(1, ctx.getText().length() - 1);
            }
        }

        @Override
        public void exitArrayQuery(ArrayQueryContext ctx) {
            var index = Integer.valueOf(ctx.NUMBER().getText());
            var field = fieldName2Text(ctx.fieldName());
            expressions.add(n -> {
                if (!n.hasField(field)) {
                    return emptyNode();
                }
                var arr = n.get(field);
                if (!arr.isArray()) {
                    return emptyNode();
                } else {
                    if (index >= 0 && index < arr.lenght()) {
                        return arr.at(index);
                    } else if (index < 0 && -index < arr.lenght()) {
                        return arr.at(arr.lenght() + index);
                    } else {
                        return emptyNode();
                    }
                }
            });
        }

        @Override
        public void exitFieldQuery(FieldQueryContext ctx) {
            var fieldNames = ctx.fieldName().stream().map(ExpressionBuilder::fieldName2Text).toList();
            expressions.add(n -> {
                var currNode = n;
                for (var field : fieldNames) {
                    if (currNode.isEmpty()) {
                        break;
                    } else if (currNode.hasField(field)) {
                        currNode = currNode.get(field);
                    } else {
                        currNode = emptyNode();
                    }
                }
                return currNode;
            });
        }

        public List<Expression> getExpressions() {
            return expressions;
        }
    }

    public static JSONata parse(String expression) {
        var parser = new JSONataQueriesParser(
                new CommonTokenStream(new JSONataQueriesLexer(CharStreams.fromString(expression))));
        var walker = new ParseTreeWalker();

        var builder = new ExpressionBuilder();
        walker.walk(builder, parser.queries());
        return new JSONata(builder.getExpressions());
    }
}
