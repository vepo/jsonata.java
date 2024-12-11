package dev.vepo.jsonata.expression;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.JSONata;
import dev.vepo.jsonata.Node;
import dev.vepo.jsonata.JSONata.Expression;
import dev.vepo.jsonata.expression.generated.JSONataQueriesBaseListener;
import dev.vepo.jsonata.expression.generated.JSONataQueriesLexer;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser;
import dev.vepo.jsonata.expression.generated.JSONataQueriesParser.FieldQueryContext;

public class JSONataExpression {

    private static class ExpressionBuilder extends JSONataQueriesBaseListener {
        private List<Expression> expressions;

        ExpressionBuilder() {
            expressions = new ArrayList<>();
        }

        @Override
        public void exitFieldQuery(FieldQueryContext ctx) {
            var fieldNames = ctx.IDENTIFIER().stream().map(f -> f.getText()).toList();
            expressions.add(n -> {
                var currNode = n;
                for (var field : fieldNames) {
                    if (currNode.isEmpty()) {
                        break;
                    } else if (currNode.hasField(field)) {
                        currNode = currNode.get(field);
                    } else {
                        currNode = Node.empty();
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
        var parser =
                new JSONataQueriesParser(new CommonTokenStream(new JSONataQueriesLexer(CharStreams.fromString(expression))));
        var walker = new ParseTreeWalker();

        var builder = new ExpressionBuilder();
        walker.walk(builder, parser.queries());
        // Suite suite = creator.getTestSuite();
        return new JSONata(builder.getExpressions());
    }
}
