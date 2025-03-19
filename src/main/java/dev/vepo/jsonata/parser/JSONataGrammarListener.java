package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.AlgebraicOperation;
import dev.vepo.jsonata.functions.AlgebraicOperator;
import dev.vepo.jsonata.functions.ArrayCast;
import dev.vepo.jsonata.functions.ArrayConstructor;
import dev.vepo.jsonata.functions.ArrayExpansion;
import dev.vepo.jsonata.functions.ArrayIndex;
import dev.vepo.jsonata.functions.ArrayQuery;
import dev.vepo.jsonata.functions.BlockContext;
import dev.vepo.jsonata.functions.BooleanExpression;
import dev.vepo.jsonata.functions.BooleanOperator;
import dev.vepo.jsonata.functions.CompareOperator;
import dev.vepo.jsonata.functions.CompareValues;
import dev.vepo.jsonata.functions.Concatenation;
import dev.vepo.jsonata.functions.ContextValue;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.DeepFind;
import dev.vepo.jsonata.functions.FieldContent;
import dev.vepo.jsonata.functions.FieldMap;
import dev.vepo.jsonata.functions.InlineIf;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.MappingJoin;
import dev.vepo.jsonata.functions.ObjectBuilder;
import dev.vepo.jsonata.functions.ObjectMapper;
import dev.vepo.jsonata.functions.UserDefinedFunction;
import dev.vepo.jsonata.functions.Wildcard;
import dev.vepo.jsonata.functions.generated.JSONataGrammarBaseListener;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.AlgebraicExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.AllDescendantSearchContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayConstructorContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayExpansionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayIndexQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.BlockExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.BooleanCompareContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.BooleanExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.BooleanValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ConcatValuesContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ContextRefereceContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ContextValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpNumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldListContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldValuesContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FloatValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FunctionCallContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FunctionDeclarationBuilderContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FunctionFeedContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.IdentifierContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.InlineIfExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.NumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectBuilderContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectConstructorContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectMapperContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ParameterStatementContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.PathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RegexValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RootPathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.StringValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ToArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.VariableAssignmentContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.VariableUsageContext;
import dev.vepo.jsonata.functions.json.JsonFactory;

public class JSONataGrammarListener extends JSONataGrammarBaseListener {
    private static final String VARIABLE_NOT_DEFINED_IN_BLOCK = "Variable should only be defined in blocks!";

    private static final Logger logger = LoggerFactory.getLogger(JSONataGrammarListener.class);

    private static String fieldName2Text(TerminalNode ctx) {
        if (!ctx.getText().startsWith("`")) {
            return ctx.getText();
        } else {
            return ctx.getText().substring(1, ctx.getText().length() - 1);
        }
    }

    private static String sanitise(String str) {
        if (str.length() > 1 && ((str.startsWith("`") && str.endsWith("`")) || (str.startsWith("\"") && str.endsWith("\""))
                || (str.startsWith("'") && str.endsWith("'")))) {
            return unescapeJson(str.substring(1, str.length() - 1));
        } else {
            return unescapeJson(str);
        }
    }

    private final Deque<Mapping> expressions;
    private final Deque<DeclaredFunction> functionsDeclared;
    private final Queue<BlockContext> blocks;

    public JSONataGrammarListener() {
        this.expressions = new LinkedList<>();
        this.functionsDeclared = new LinkedList<>();
        this.blocks = new LinkedList<>();
    }

    @Override
    public void enterFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        logger.atInfo().setMessage("[ENTER] [BEGIN] Enter Function Declaration! {}").addArgument(ctx::getText).log();
        blocks.offer(new BlockContext(blocks));
        logger.atInfo().setMessage("[ENTER] [END  ] Enter Function Declaration! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Exit Function declaration builder! {}").addArgument(ctx::getText).log();
        this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.FV_NAME()
                                                                  .stream()
                                                                  .map(TerminalNode::getText)
                                                                  .toList(),
                                                               this.blocks.poll(),
                                                               this.expressions.removeLast()));
        logger.atInfo().setMessage("[EXIT] [END  ] Exit Function declaration builder! {}").addArgument(expressions).log();
    }

    @Override
    public void enterFunctionFeed(FunctionFeedContext ctx) {
        logger.atInfo().setMessage("[ENTER] [BEGIN] Enter Function feed! {}").addArgument(ctx::getText).log();
        logger.atInfo().setMessage("[ENTER] [END  ] Enter Function feed! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionFeed(FunctionFeedContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Function feed! {}").addArgument(ctx::getText).log();
        var valueProviders = previousExpressions((int) ctx.functionStatement()
                                                          .parameterStatement()
                                                          .stream()
                                                          .map(ParameterStatementContext::expression)
                                                          .filter(Objects::nonNull)
                                                          .count());
        valueProviders.addFirst(expressions.removeLast());
        var functions = previousFunctions((int) ctx.functionStatement()
                                                   .parameterStatement()
                                                   .stream()
                                                   .map(ParameterStatementContext::functionDeclaration)
                                                   .filter(Objects::nonNull)
                                                   .count());
        var fnName = ctx.functionStatement().FV_NAME().getText();
        expressions.offer(BuiltInFunction.get(fnName)
                                         .map(fn -> fn.instantiate(valueProviders, functions))
                                         .orElseGet(() -> Optional.ofNullable(this.blocks.peek())
                                                                  .flatMap(block -> block.function(fnName))
                                                                  .map(fn -> new UserDefinedFunction(valueProviders, fn))
                                                                  .orElseThrow(() -> new JSONataException("Function not found: " + fnName))));
        logger.atInfo().setMessage("[EXIT] [END  ] Function feed! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        logger.atInfo().setMessage("Function call! {}").addArgument(ctx::getText).log();
        var valueProviders = previousExpressions((int) ctx.functionStatement()
                                                          .parameterStatement()
                                                          .stream()
                                                          .map(ParameterStatementContext::expression)
                                                          .filter(Objects::nonNull)
                                                          .count());
        var functions = previousFunctions((int) ctx.functionStatement()
                                                   .parameterStatement()
                                                   .stream()
                                                   .map(ParameterStatementContext::functionDeclaration)
                                                   .filter(Objects::nonNull)
                                                   .count());
        var fnName = ctx.functionStatement().FV_NAME().getText();
        expressions.offer(BuiltInFunction.get(fnName)
                                         .map(fn -> fn.instantiate(valueProviders, functions))
                                         .orElseGet(() -> Optional.ofNullable(this.blocks.peek())
                                                                  .flatMap(block -> block.function(fnName))
                                                                  .map(fn -> new UserDefinedFunction(valueProviders, fn))
                                                                  .orElseThrow(() -> new JSONataException("Function not found: " + fnName))));
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        logger.atInfo().setMessage("Root path! {}").addArgument(ctx::getText).log();
        expressions.offer((original, value) -> value);
    }

    @Override
    public void exitIdentifier(IdentifierContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Identifier! {}").addArgument(ctx::getText).log();
        expressions.offer(new FieldMap(fieldName2Text(ctx.IDENTIFIER())));
        logger.atInfo().setMessage("[EXIT] [END  ] Identifier! {}").addArgument(expressions).log();
    }

    @Override
    public void exitPath(PathContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Path! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new MappingJoin(previousFunction,currentFunction));
        logger.atInfo().setMessage("[EXIT] [END  ] Path! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFieldValues(FieldValuesContext ctx) {
        logger.atInfo().setMessage("Field values! {}").addArgument(ctx::getText).log();
        expressions.offer(new Wildcard());
    }

    @Override
    public void exitAllDescendantSearch(AllDescendantSearchContext ctx) {
        logger.atInfo().setMessage("All descendant search! {}").addArgument(ctx::getText).log();
        expressions.offer(new DeepFind());
    }

    @Override
    public void exitToArray(ToArrayContext ctx) {
        logger.atInfo().setMessage("To array! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        expressions.offer(currentFunction.andThen(new ArrayCast()));
    }

    @Override
    public void exitArrayIndexQuery(ArrayIndexQueryContext ctx) {
        logger.atInfo().setMessage("Array index query! {}").addArgument(ctx::getText).log();
        if (expressions.isEmpty()) {
            expressions.offer(new ArrayIndex(Integer.valueOf(ctx.NUMBER().getText())));
        } else {
            var previousFunction = expressions.removeLast();
            expressions.offer(previousFunction.andThen(new ArrayIndex(Integer.valueOf(ctx.NUMBER().getText()))));
        }
    }

    @Override
    public void exitBooleanCompare(BooleanCompareContext ctx) {
        logger.atInfo().setMessage("Boolean compare! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new CompareValues(previousFunction, CompareOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitBooleanExpression(BooleanExpressionContext ctx) {
        logger.atInfo().setMessage("Boolean expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new BooleanExpression(previousFunction, BooleanOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitAlgebraicExpression(AlgebraicExpressionContext ctx) {
        logger.atInfo().setMessage("Algebraic expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new AlgebraicOperation(previousFunction, AlgebraicOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitInlineIfExpression(InlineIfExpressionContext ctx) {
        logger.atInfo().setMessage("Inline if expression! {}").addArgument(ctx::getText).log();
        var falseValueProvider = Optional.ofNullable(ctx.expression().size() == 3 ? expressions.removeLast() : null);
        var trueValueProvider = expressions.removeLast();
        var testProvider = expressions.removeLast();
        this.expressions.offer(new InlineIf(testProvider, trueValueProvider, falseValueProvider));
    }

    @Override
    public void exitArrayQuery(ArrayQueryContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Array query! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new ArrayQuery(previousFunction, currentFunction));
        logger.atInfo().setMessage("[EXIT] [END  ] Array query! {}").addArgument(ctx::getText).log();
    }

    @Override
    public void exitStringValue(StringValueContext ctx) {
        logger.atInfo().setMessage("String value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> stringValue(sanitise(ctx.getText())));
    }

    @Override
    public void exitNumberValue(NumberValueContext ctx) {
        logger.atInfo().setMessage("Number value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Integer.valueOf(ctx.getText())));
    }

    @Override
    public void exitFloatValue(FloatValueContext ctx) {
        logger.atInfo().setMessage("Float value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Double.valueOf(ctx.getText())));
    }

    @Override
    public void exitExpNumberValue(ExpNumberValueContext ctx) {
        logger.atInfo().setMessage("Exp number value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Float.valueOf(ctx.getText())));
    }

    @Override
    public void exitBooleanValue(BooleanValueContext ctx) {
        logger.atInfo().setMessage("Boolean value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> booleanValue(Boolean.valueOf(ctx.getText())));
    }

    @Override
    public void exitContextValue(ContextValueContext ctx) {
        logger.atInfo().setMessage("Context value! {}").addArgument(ctx::getText).log();
        var contextFunction = expressions.removeLast();
        this.expressions.offer(new ContextValue(contextFunction));
    }

    @Override
    public void exitContextReferece(ContextRefereceContext ctx) {
        logger.atInfo().setMessage("Context reference! {}").addArgument(ctx::getText).log();
        this.expressions.offer((original, value) -> value);
    }

    @Override
    public void exitConcatValues(ConcatValuesContext ctx) {
        logger.atInfo().setMessage("Concat values! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new Concatenation(previousFunction, currentFunction));
    }

    @Override
    public void exitArrayConstructor(ArrayConstructorContext ctx) {
        logger.atInfo().setMessage("Array constructor! {}").addArgument(ctx::getText).log();
        var expresisonCounter = ctx.expressionList().expression().size();
        var fns = new ArrayList<Mapping>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            fns.addFirst(expressions.removeLast());
        }
        expressions.offer(new ArrayConstructor(fns));
    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        logger.atInfo().setMessage("Object mapper! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(previousFunction.andThen(new ObjectMapper(fieldList)));
    }

    @Override
    public void exitObjectConstructor(ObjectConstructorContext ctx) {
        logger.atInfo().setMessage("Object constructor! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(previousFunction.andThen(new ObjectBuilder(fieldList)));
    }

    @Override
    public void exitObjectBuilder(ObjectBuilderContext ctx) {
        logger.atInfo().setMessage("Object builder! {}").addArgument(ctx::getText).log();
        expressions.offer(new ObjectBuilder(objectFields(ctx.fieldList())));
    }

    @Override
    public void exitBlockExpression(BlockExpressionContext ctx) {
        logger.atInfo().setMessage("Exit block declaration! {}").addArgument(ctx::getText).log();
        this.blocks.poll();
    }

    @Override
    public void enterBlockExpression(BlockExpressionContext ctx) {
        logger.atInfo().setMessage("Enter Block expression! {}").addArgument(ctx::getText).log();
        this.blocks.offer(new BlockContext(this.blocks));
    }

    @Override
    public void exitVariableUsage(VariableUsageContext ctx) {
        logger.atInfo().setMessage("Variable usage! {}").addArgument(ctx::getText).log();
        var block = Objects.requireNonNull(this.blocks.peek(), VARIABLE_NOT_DEFINED_IN_BLOCK);
        var variableName = ctx.FV_NAME().getText();
        expressions.offer((original, current) -> block.variable(variableName)
                                                      .orElseThrow(() -> new JSONataException("Variable not found: " + variableName))
                                                      .map(original, current));
    }

    @Override
    public void exitRegexValue(RegexValueContext ctx) {
        logger.atInfo().setMessage("Regex! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> JsonFactory.regex(ctx.getText()));
    }

    @Override
    public void exitVariableAssignment(VariableAssignmentContext ctx) {
        logger.atInfo().setMessage("Variable assignment! {}").addArgument(ctx::getText).log();
        if (Objects.nonNull(ctx.expression())) {
            Objects.requireNonNull(this.blocks.peek(), VARIABLE_NOT_DEFINED_IN_BLOCK)
                   .defineVariable(ctx.FV_NAME().getText(), expressions.removeLast());
        } else {
            Objects.requireNonNull(this.blocks.peek(), VARIABLE_NOT_DEFINED_IN_BLOCK)
                   .defineFunction(ctx.FV_NAME().getText(), functionsDeclared.removeLast());
        }
    }

    @Override
    public void exitArrayExpansion(ArrayExpansionContext ctx) {
        logger.atInfo().setMessage("[EXIT] [BEGIN] Array Expansion! {}").addArgument(expressions).log();;
        var rightExpression = expressions.removeLast();
        var leftExpression = expressions.removeLast();
        expressions.offer(new ArrayExpansion(leftExpression, rightExpression));
        logger.atInfo().setMessage("[EXIT] [END  ] Array Expansion! {}").addArgument(expressions).log();;
    }

    public List<Mapping> getExpressions() {
        return expressions.stream().toList();
    }

    private List<DeclaredFunction> previousFunctions(int size) {
        var fns = new ArrayList<DeclaredFunction>(size);
        for (int i = 0; i < size; ++i) {
            fns.addFirst(functionsDeclared.removeLast());
        }
        return fns;
    }

    private List<Mapping> previousExpressions(int size) {
        var fns = new ArrayList<Mapping>(size);
        for (int i = 0; i < size; ++i) {
            fns.addFirst(expressions.removeLast());
        }
        return fns;
    }

    private List<FieldContent> objectFields(FieldListContext ctx) {
        var expresisonCounter = ctx.expression().size();
        var fieldBuilder = new ArrayList<FieldContent>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            var valueFn = expressions.removeLast();
            var fieldFn = expressions.removeLast();
            fieldBuilder.addFirst(new FieldContent(fieldFn, valueFn, Objects.isNull(ctx.uniqueObj(i).DOLLAR())));
        }
        return fieldBuilder;
    }
}