package dev.vepo.jsonata.functions;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.functions.generated.MappingExpressionsLexer;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser;
import dev.vepo.jsonata.parser.JSONataValidator;
import dev.vepo.jsonata.parser.MappingExpressionsListener;

public class MappingParser {

    public static List<Mapping> parse(String content) {
        var validator = new JSONataValidator();
        var lexer = new MappingExpressionsLexer(CharStreams.fromString(content));
        lexer.addErrorListener(validator);
        var parser = new MappingExpressionsParser(new CommonTokenStream(lexer));
        parser.addErrorListener(validator);
        var walker = new ParseTreeWalker();

        var builder = new MappingExpressionsListener();
        walker.walk(builder, parser.expressions());
        return builder.getExpressions();
    }

    private MappingParser() {
    }

}
