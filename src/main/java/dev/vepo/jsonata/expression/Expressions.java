package dev.vepo.jsonata.expression;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.expression.generated.ExpressionsLexer;
import dev.vepo.jsonata.expression.generated.ExpressionsParser;

public class Expressions {

    public static List<Expression> parse(String content) {
        var validator = new ExpressionValidator();
        var lexer = new ExpressionsLexer(CharStreams.fromString(content));
        lexer.addErrorListener(validator);
        var parser = new ExpressionsParser(new CommonTokenStream(lexer));
        parser.addErrorListener(validator);
        var walker = new ParseTreeWalker();

        var builder = new ExpressionBuilder();
        walker.walk(builder, parser.expressions());
        return builder.getExpressions();
    }

    private Expressions() {
    }

}
