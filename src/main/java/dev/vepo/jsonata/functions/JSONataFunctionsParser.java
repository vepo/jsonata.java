package dev.vepo.jsonata.functions;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.parser.ExpressionBuilder;
import dev.vepo.jsonata.parser.ExpressionValidator;
import dev.vepo.jsonata.functions.generated.JSONataGrammarLexer;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser;

public class JSONataFunctionsParser {

    public static List<JSONataFunction> parse(String content) {
        var validator = new ExpressionValidator();
        var lexer = new JSONataGrammarLexer(CharStreams.fromString(content));
        lexer.addErrorListener(validator);
        var parser = new JSONataGrammarParser(new CommonTokenStream(lexer));
        parser.addErrorListener(validator);
        var walker = new ParseTreeWalker();

        var builder = new ExpressionBuilder();
        walker.walk(builder, parser.expressions());
        return builder.getExpressions();
    }

    private JSONataFunctionsParser() {
    }

}