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

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.RegisteredFunction;
import dev.vepo.jsonata.functions.AlgebraicOperation;
import dev.vepo.jsonata.functions.Coalesce;
import dev.vepo.jsonata.functions.ContextBind;
import dev.vepo.jsonata.functions.OrderBy;
import dev.vepo.jsonata.functions.OrderBy.OrderKey;
import dev.vepo.jsonata.functions.ParentReference;
import dev.vepo.jsonata.functions.PathBindings;
import dev.vepo.jsonata.functions.PositionalBind;
import dev.vepo.jsonata.functions.Transform;
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
import dev.vepo.jsonata.functions.builtin.CompoundFunction;
import dev.vepo.jsonata.functions.generated.MappingExpressionsBaseListener;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.AlgebraicExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.AllDescendantSearchContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ArrayConstructorContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ArrayExpansionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ArrayIndexQueryContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ArrayQueryContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.BlockExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.BooleanCompareContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.BooleanExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.BooleanValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ChainExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.CoalesceExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ConcatValuesContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ContextBindContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ContextRefereceContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ContextValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ExpNumberValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FieldListContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FieldValuesContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FloatValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FunctionCallContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FunctionCompositionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FunctionDeclarationBuilderContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.FunctionFeedContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.IdentifierContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.InlineIfExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.NumberValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ObjectBuilderContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ObjectConstructorContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ObjectMapperContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.OrderByExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.OrderKeyContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ParameterStatementContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ParentReferenceContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.PositionalBindContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.PathContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.RegexValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.RootPathContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.StringValueContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.ToArrayContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.TransformDefinitionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.TransformExpressionContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.VariableAssignmentContext;
import dev.vepo.jsonata.functions.generated.MappingExpressionsParser.VariableUsageContext;
import dev.vepo.jsonata.functions.json.JsonFactory;

public class MappingExpressionsListener extends MappingExpressionsBaseListener {
    private static final String VARIABLE_NOT_DEFINED_IN_BLOCK = "Variable should only be defined in blocks!";

    private static final Logger logger = LoggerFactory.getLogger(MappingExpressionsListener.class);

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
    private final EvaluationEnvironment environment;

    public MappingExpressionsListener() {
        this(EvaluationEnvironment.empty());
    }

    public MappingExpressionsListener(EvaluationEnvironment environment) {
        this.expressions = new LinkedList<>();
        this.functionsDeclared = new LinkedList<>();
        this.blocks = new LinkedList<>();
        this.environment = environment;
        if (!environment.bindings().isEmpty()) {
            blocks.offer(environment.rootBlockContext());
        }
    }

    @Override
    public void enterFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        logger.atDebug().setMessage("[ENTER] [BEGIN] Enter Function Declaration! {}").addArgument(ctx::getText).log();
        blocks.offer(new BlockContext(blocks));
        logger.atDebug().setMessage("[ENTER] [END  ] Enter Function Declaration! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Exit Function declaration builder! {}").addArgument(ctx::getText).log();
        this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.FV_NAME()
                                                                  .stream()
                                                                  .map(TerminalNode::getText)
                                                                  .toList(),
                                                               this.blocks.poll(),
                                                               this.expressions.removeLast()));
        logger.atDebug().setMessage("[EXIT] [END  ] Exit Function declaration builder! {}").addArgument(expressions).log();
    }

    @Override
    public void enterFunctionFeed(FunctionFeedContext ctx) {
        logger.atDebug().setMessage("[ENTER] [BEGIN] Enter Function feed! {}").addArgument(ctx::getText).log();
        logger.atDebug().setMessage("[ENTER] [END  ] Enter Function feed! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionFeed(FunctionFeedContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Function feed! {}").addArgument(ctx::getText).log();
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
        expressions.offer(resolveFunction(fnName, valueProviders, functions));
        logger.atDebug().setMessage("[EXIT] [END  ] Function feed! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Function call! {}").addArgument(ctx::getText).log();
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
        expressions.offer(resolveFunctionCall(fnName, valueProviders, functions));
        logger.atDebug().setMessage("[EXIT] [END  ] Function call! {}").addArgument(expressions).log();
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        logger.atDebug().setMessage("Root path! {}").addArgument(ctx::getText).log();
        expressions.offer((original, value) -> original);
    }

    @Override
    public void exitIdentifier(IdentifierContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Identifier! {}").addArgument(ctx::getText).log();
        expressions.offer(new FieldMap(fieldName2Text(ctx.IDENTIFIER())));
        logger.atDebug().setMessage("[EXIT] [END  ] Identifier! {}").addArgument(expressions).log();
    }

    @Override
    public void exitPath(PathContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Path! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new MappingJoin(previousFunction, currentFunction));
        logger.atDebug().setMessage("[EXIT] [END  ] Path! {}").addArgument(expressions).log();
    }

    @Override
    public void exitFieldValues(FieldValuesContext ctx) {
        logger.atDebug().setMessage("Field values! {}").addArgument(ctx::getText).log();
        expressions.offer(new Wildcard());
    }

    @Override
    public void exitAllDescendantSearch(AllDescendantSearchContext ctx) {
        logger.atDebug().setMessage("All descendant search! {}").addArgument(ctx::getText).log();
        expressions.offer(new DeepFind());
    }

    @Override
    public void exitToArray(ToArrayContext ctx) {
        logger.atDebug().setMessage("To array! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        expressions.offer(currentFunction.andThen(new ArrayCast()));
    }

    @Override
    public void exitArrayIndexQuery(ArrayIndexQueryContext ctx) {
        logger.atDebug().setMessage("Array index query! {}").addArgument(ctx::getText).log();
        if (expressions.isEmpty()) {
            expressions.offer(new ArrayIndex(Integer.valueOf(ctx.NUMBER().getText())));
        } else {
            var previousFunction = expressions.removeLast();
            expressions.offer(previousFunction.andThen(new ArrayIndex(Integer.valueOf(ctx.NUMBER().getText()))));
        }
    }

    @Override
    public void exitBooleanCompare(BooleanCompareContext ctx) {
        logger.atDebug().setMessage("Boolean compare! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new CompareValues(previousFunction, CompareOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitBooleanExpression(BooleanExpressionContext ctx) {
        logger.atDebug().setMessage("Boolean expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new BooleanExpression(previousFunction, BooleanOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitAlgebraicExpression(AlgebraicExpressionContext ctx) {
        logger.atDebug().setMessage("Algebraic expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new AlgebraicOperation(previousFunction, AlgebraicOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitInlineIfExpression(InlineIfExpressionContext ctx) {
        logger.atDebug().setMessage("Inline if expression! {}").addArgument(ctx::getText).log();
        var falseValueProvider = Optional.ofNullable(ctx.expression().size() == 3 ? expressions.removeLast() : null);
        var trueValueProvider = expressions.removeLast();
        var testProvider = expressions.removeLast();
        this.expressions.offer(new InlineIf(testProvider, trueValueProvider, falseValueProvider));
    }

    @Override
    public void exitArrayQuery(ArrayQueryContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Array query! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new ArrayQuery(previousFunction, currentFunction));
        logger.atDebug().setMessage("[EXIT] [END  ] Array query! {}").addArgument(ctx::getText).log();
    }

    @Override
    public void exitStringValue(StringValueContext ctx) {
        logger.atDebug().setMessage("String value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> stringValue(sanitise(ctx.getText())));
    }

    @Override
    public void exitNumberValue(NumberValueContext ctx) {
        logger.atDebug().setMessage("Number value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Integer.valueOf(ctx.getText())));
    }

    @Override
    public void exitFloatValue(FloatValueContext ctx) {
        logger.atDebug().setMessage("Float value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Double.valueOf(ctx.getText())));
    }

    @Override
    public void exitExpNumberValue(ExpNumberValueContext ctx) {
        logger.atDebug().setMessage("Exp number value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> numberValue(Float.valueOf(ctx.getText())));
    }

    @Override
    public void exitBooleanValue(BooleanValueContext ctx) {
        logger.atDebug().setMessage("Boolean value! {}").addArgument(ctx::getText).log();
        expressions.offer((original, current) -> booleanValue(Boolean.valueOf(ctx.getText())));
    }

    @Override
    public void exitContextValue(ContextValueContext ctx) {
        logger.atDebug().setMessage("Context value! {}").addArgument(ctx::getText).log();
        var contextFunction = expressions.removeLast();
        this.expressions.offer(new ContextValue(contextFunction));
    }

    @Override
    public void exitContextReferece(ContextRefereceContext ctx) {
        logger.atDebug().setMessage("Context reference! {}").addArgument(ctx::getText).log();
        this.expressions.offer((original, value) -> value);
    }

    @Override
    public void exitConcatValues(ConcatValuesContext ctx) {
        logger.atDebug().setMessage("Concat values! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new Concatenation(previousFunction, currentFunction));
    }

    @Override
    public void exitArrayConstructor(ArrayConstructorContext ctx) {
        logger.atDebug().setMessage("Array constructor! {}").addArgument(ctx::getText).log();
        var expresisonCounter = ctx.expressionList().expression().size();
        var fns = new ArrayList<Mapping>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            fns.addFirst(expressions.removeLast());
        }
        expressions.offer(new ArrayConstructor(fns));
    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        logger.atDebug().setMessage("Object mapper! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(previousFunction.andThen(new ObjectMapper(fieldList)));
    }

    @Override
    public void exitObjectConstructor(ObjectConstructorContext ctx) {
        logger.atDebug().setMessage("Object constructor! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(previousFunction.andThen(new ObjectBuilder(fieldList)));
    }

    @Override
    public void exitObjectBuilder(ObjectBuilderContext ctx) {
        logger.atDebug().setMessage("Object builder! {}").addArgument(ctx::getText).log();
        expressions.offer(new ObjectBuilder(objectFields(ctx.fieldList())));
    }

    @Override
    public void exitBlockExpression(BlockExpressionContext ctx) {
        logger.atDebug().setMessage("Exit block declaration! {}").addArgument(ctx::getText).log();
        this.blocks.poll();
    }

    @Override
    public void enterBlockExpression(BlockExpressionContext ctx) {
        logger.atDebug().setMessage("Enter Block expression! {}").addArgument(ctx::getText).log();
        this.blocks.offer(new BlockContext(this.blocks));
    }

    @Override
    public void exitVariableUsage(VariableUsageContext ctx) {
        logger.atDebug().setMessage("Variable usage! {}").addArgument(ctx::getText).log();
        var block = this.blocks.peek();
        var variableName = ctx.FV_NAME().getText();
        Mapping bindingAtParse = null;
        if (block != null) {
            bindingAtParse = block.variable(variableName).orElse(null);
        }
        final Mapping capturedBinding = bindingAtParse;
        expressions.offer((original, current) -> {
            var pathBinding = PathBindings.binding(variableName);
            if (pathBinding.isPresent()) {
                return pathBinding.get();
            }
            if (capturedBinding != null) {
                return capturedBinding.map(original, current);
            }
            if (block != null) {
                return block.variable(variableName)
                            .orElseThrow(() -> new JSONataException("Variable not found: " + variableName))
                            .map(original, current);
            }
            return BuiltInFunction.get(variableName)
                                  .map(bif -> bif.instantiate(List.of((o, c) -> current), List.of())
                                                   .map(original, current))
                                  .orElseThrow(() -> new JSONataException("Variable not found: " + variableName));
        });
    }

    @Override
    public void exitCoalesceExpression(CoalesceExpressionContext ctx) {
        logger.atDebug().setMessage("Coalesce expression! {}").addArgument(ctx::getText).log();
        var right = expressions.removeLast();
        var left = expressions.removeLast();
        expressions.offer(new Coalesce(left, right));
    }

    @Override
    public void exitOrderByExpression(OrderByExpressionContext ctx) {
        logger.atDebug().setMessage("Order-by expression! {}").addArgument(ctx::getText).log();
        var keys = new ArrayList<OrderKey>(ctx.orderKey().size());
        for (int i = ctx.orderKey().size() - 1; i >= 0; i--) {
            keys.addFirst(orderKeyFromContext(ctx.orderKey(i)));
        }
        var operand = expressions.removeLast();
        expressions.offer(new OrderBy(operand, keys));
    }

    @Override
    public void exitParentReference(ParentReferenceContext ctx) {
        logger.atDebug().setMessage("Parent reference! {}").addArgument(ctx::getText).log();
        expressions.offer(new ParentReference());
    }

    @Override
    public void exitPositionalBind(PositionalBindContext ctx) {
        logger.atDebug().setMessage("Positional bind! {}").addArgument(ctx::getText).log();
        var operand = expressions.removeLast();
        expressions.offer(new PositionalBind(operand, ctx.FV_NAME().getText()));
    }

    @Override
    public void exitContextBind(ContextBindContext ctx) {
        logger.atDebug().setMessage("Context bind! {}").addArgument(ctx::getText).log();
        var operand = expressions.removeLast();
        expressions.offer(new ContextBind(operand, ctx.FV_NAME().getText()));
    }

    @Override
    public void exitTransformExpression(TransformExpressionContext ctx) {
        logger.atDebug().setMessage("Transform expression! {}").addArgument(ctx::getText).log();
        expressions.offer(buildTransform(ctx.transformDefinition()));
    }

    @Override
    public void exitChainExpression(ChainExpressionContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Chain expression! {}").addArgument(ctx::getText).log();
        var target = ctx.chainTarget();
        if (target.transformDefinition() != null) {
            var left = expressions.removeLast();
            var transform = buildTransform(target.transformDefinition());
            expressions.offer((original, current) -> transform.map(original, left.map(original, current)));
        } else {
            var valueProviders = previousExpressions((int) target.functionStatement()
                                                                  .parameterStatement()
                                                                  .stream()
                                                                  .map(ParameterStatementContext::expression)
                                                                  .filter(Objects::nonNull)
                                                                  .count());
            var left = expressions.removeLast();
            valueProviders.addFirst(left);
            var functions = previousFunctions((int) target.functionStatement()
                                                       .parameterStatement()
                                                       .stream()
                                                       .map(ParameterStatementContext::functionDeclaration)
                                                       .filter(Objects::nonNull)
                                                       .count());
            var fnName = target.functionStatement().FV_NAME().getText();
            expressions.offer(resolveFunctionCall(fnName, valueProviders, functions));
        }
        logger.atDebug().setMessage("[EXIT] [END  ] Chain expression! {}").addArgument(expressions).log();
    }

    private Transform buildTransform(TransformDefinitionContext ctx) {
        var deleteMapping = ctx.expression().size() == 3
                ? Optional.of(expressions.removeLast())
                : Optional.<Mapping>empty();
        var update = expressions.removeLast();
        var pattern = expressions.removeLast();
        return new Transform(pattern, update, deleteMapping);
    }

    private OrderKey orderKeyFromContext(OrderKeyContext ctx) {
        var descending = ctx.getChildCount() > 1 && ">".equals(ctx.getChild(0).getText());
        return new OrderKey(expressions.removeLast(), descending);
    }

    @Override
    public void exitFunctionComposition(FunctionCompositionContext ctx) {
        logger.atDebug().setMessage("[EXIT] [BEGIN] Function composition! {}").addArgument(ctx::getText).log();
        var fnNames = ctx.FV_NAME().stream().skip(1l).toList();
        var fnName = ctx.FV_NAME().get(0).getText();
        var block = Objects.requireNonNull(this.blocks.peek(), VARIABLE_NOT_DEFINED_IN_BLOCK);
        var context = new CompoundFunction.CompoundContext();
        block.defineCompoundFunction(fnName, new CompoundFunction(fnNames.stream().map(TerminalNode::getText).map(BuiltInFunction::get).map(maybeFn -> {
            if (maybeFn.isPresent()) {
                return maybeFn.get().instantiate(List.of((original, current) -> context.getPreviousFunctionResult()), List.of());
            } else {
                throw new JSONataException("Function not found: " + fnName);
            }
        }).toList(), context));
        logger.atDebug().setMessage("[EXIT] [END  ] Function composition! {}").addArgument(expressions).log();
    }

    @Override
    public void exitRegexValue(RegexValueContext ctx) {
        logger.atDebug().setMessage("Regex! {}").addArgument(ctx::getText).log();
        var regex = JsonFactory.regex(ctx.getText());
        expressions.offer((original, current) -> regex);
    }

    @Override
    public void exitVariableAssignment(VariableAssignmentContext ctx) {
        logger.atDebug().setMessage("Variable assignment! {}").addArgument(ctx::getText).log();
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
        logger.atDebug().setMessage("[EXIT] [BEGIN] Array Expansion! {}").addArgument(expressions).log();
        ;
        var rightExpression = expressions.removeLast();
        var leftExpression = expressions.removeLast();
        expressions.offer(new ArrayExpansion(leftExpression, rightExpression));
        logger.atDebug().setMessage("[EXIT] [END  ] Array Expansion! {}").addArgument(expressions).log();
        ;
    }

    public List<Mapping> getExpressions() {
        return expressions.stream().toList();
    }

    private Mapping resolveFunction(String fnName, List<Mapping> valueProviders, List<DeclaredFunction> functions) {
        return BuiltInFunction.get(fnName)
                              .map(fn -> fn.instantiate(valueProviders, functions))
                              .orElseGet(() -> resolveFunctionCall(fnName, valueProviders, functions));
    }

    private Mapping resolveFunctionCall(String fnName, List<Mapping> valueProviders, List<DeclaredFunction> functions) {
        return BuiltInFunction.get(fnName)
                              .map(fn -> fn.instantiate(valueProviders, functions))
                              .or(() -> Optional.ofNullable(this.blocks.peek())
                                                .flatMap(block -> block.function(fnName))
                                                .map(fn -> new UserDefinedFunction(valueProviders, fn)))
                              .or(() -> Optional.ofNullable(environment.functions().get(fnName))
                                                .map(fn -> new RegisteredFunction(fnName, valueProviders, environment)))
                              .orElseGet(() -> Optional.ofNullable(this.blocks.peek())
                                                       .flatMap(block -> block.compoundFunction(fnName))
                                                       .map(fn -> fn.withProviders(valueProviders))
                                                       .orElseThrow(() -> new JSONataException("Function not found: " + fnName)));
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