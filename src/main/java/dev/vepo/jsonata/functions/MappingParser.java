package dev.vepo.jsonata.functions;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.functions.generated.MappingExpressionsLexer;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser;
import dev.vepo.jsonata.parser.JSONataValidator;
import dev.vepo.jsonata.parser.MappingExpressionsListener;

/**
 * Parses JSONata expression text into an executable list of {@link Mapping} nodes.
 *
 * <p>Infrastructure entry point for the compile phase: lexes and parses the grammar,
 * walks the parse tree with {@link MappingExpressionsListener}, then runs
 * {@link TailCallOptimizer} on the result. Parsing errors are collected by
 * {@link JSONataValidator} and surfaced as parse failures.
 *
 * @see Mapping
 * @see EvaluationEnvironment
 */
public class MappingParser {

    /**
     * Parses a JSONata expression with no registered external functions.
     *
     * @param content the expression text
     * @return compiled mappings ready for evaluation (typically one top-level mapping)
     */
    public static List<Mapping> parse(String content) {
        return parse(content, EvaluationEnvironment.empty());
    }

    /**
     * Parses a JSONata expression with the given embedding environment.
     *
     * @param content     the expression text
     * @param environment registered functions and other embedding hooks
     * @return compiled mappings ready for evaluation
     */
    public static List<Mapping> parse(String content, EvaluationEnvironment environment) {
        var validator = new JSONataValidator();
        var lexer = new MappingExpressionsLexer(CharStreams.fromString(content));
        lexer.addErrorListener(validator);
        var parser = new MappingExpressionsParser(new CommonTokenStream(lexer));
        parser.addErrorListener(validator);
        var walker = new ParseTreeWalker();

        var builder = new MappingExpressionsListener(environment);
        walker.walk(builder, parser.expressions());
        return TailCallOptimizer.optimize(builder.getExpressions());
    }

    private MappingParser() {
    }
}
