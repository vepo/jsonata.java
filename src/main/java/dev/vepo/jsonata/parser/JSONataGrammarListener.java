package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.AlgebraicJSONataFunction;
import dev.vepo.jsonata.functions.AlgebraicOperator;
import dev.vepo.jsonata.functions.ArrayCastTransformerJSONataFunction;
import dev.vepo.jsonata.functions.ArrayConstructorJSONataFunction;
import dev.vepo.jsonata.functions.ArrayIndexJSONataFunction;
import dev.vepo.jsonata.functions.ArrayQueryJSONataFunction;
import dev.vepo.jsonata.functions.ArrayRangeJSONataFunction;
import dev.vepo.jsonata.functions.BooleanExpressionJSONataFunction;
import dev.vepo.jsonata.functions.BooleanOperator;
import dev.vepo.jsonata.functions.BuiltInSortJSONataFunction;
import dev.vepo.jsonata.functions.BuiltInSumJSONataFunction;
import dev.vepo.jsonata.functions.CompareOperator;
import dev.vepo.jsonata.functions.CompareValuesJSONataFunction;
import dev.vepo.jsonata.functions.ContextValueJSONataFunction;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.DeepFindByFieldNameJSONataFunction;
import dev.vepo.jsonata.functions.FieldContent;
import dev.vepo.jsonata.functions.FieldMapJSONataFunction;
import dev.vepo.jsonata.functions.InlineIfJSONataFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.JoinJSONataFunction;
import dev.vepo.jsonata.functions.ObjectBuilderJSONataFunction;
import dev.vepo.jsonata.functions.ObjectMapperJSONataFunction;
import dev.vepo.jsonata.functions.StringConcatJSONataFunction;
import dev.vepo.jsonata.functions.WildcardJSONataFunction;
import dev.vepo.jsonata.functions.generated.JSONataGrammarBaseListener;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.AlgebraicExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.AllDescendantSearchContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayConstructorContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayIndexQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayQueryContext;
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
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.IdentifierContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.InlineIfExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.NumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectBuilderContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectConstructorContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectMapperContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.PathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RangeQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RootPathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.StringValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ToArrayContext;

public class JSONataGrammarListener extends JSONataGrammarBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(JSONataGrammarListener.class);

    public enum BuiltInFunction {
        SORT("$sort"),
        SUM("$sum");

        public static BuiltInFunction get(String name) {
            return Stream.of(values())
                         .filter(n -> n.name.compareToIgnoreCase(name) == 0)
                         .findAny()
                         .orElseThrow(() -> new JSONataException(String.format("Unknown function!!! function=%s", name)));
        }

        private String name;

        BuiltInFunction(String name) {
            this.name = name;
        }
    }

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

    private final Deque<JSONataFunction> expressions;

    private final Deque<DeclaredFunction> functionsDeclared;

    public JSONataGrammarListener() {
        this.expressions = new LinkedList<>();
        this.functionsDeclared = new LinkedList<>();
    }

    @Override
    public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        logger.atInfo().setMessage("Function declaration builder! {}").addArgument(ctx::getText).log();
        this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.IDENTIFIER()
                                                                  .stream()
                                                                  .map(TerminalNode::getText)
                                                                  .toList(),
                                                               this.expressions.removeLast()));
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        logger.atInfo().setMessage("Function call! {}").addArgument(ctx::getText).log();
        var valueProvider = expressions.removeLast();
        Optional<DeclaredFunction> maybeFn = functionsDeclared.isEmpty() ? Optional.empty() : Optional.of(functionsDeclared.removeLast());
        expressions.offer(switch (BuiltInFunction.get(ctx.functionStatement().IDENTIFIER().getText())) {
            case SORT -> new BuiltInSortJSONataFunction(valueProvider, maybeFn);
            case SUM -> new BuiltInSumJSONataFunction(valueProvider);
        });
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        logger.atInfo().setMessage("Root path! {}").addArgument(ctx::getText).log();
        expressions.offer((original, value) -> value);
    }

    @Override
    public void exitIdentifier(IdentifierContext ctx) {
        logger.atInfo().setMessage("Identifier! {}").addArgument(ctx::getText).log();
        expressions.offer(new FieldMapJSONataFunction(fieldName2Text(ctx.IDENTIFIER())));
    }

    @Override
    public void exitPath(PathContext ctx) {
        logger.atInfo().setMessage("Path! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, currentFunction));
    }

    @Override
    public void exitFieldValues(FieldValuesContext ctx) {
        logger.atInfo().setMessage("Field values! {}").addArgument(ctx::getText).log();
        expressions.offer(new WildcardJSONataFunction());
    }

    @Override
    public void exitAllDescendantSearch(AllDescendantSearchContext ctx) {
        logger.atInfo().setMessage("All descendant search! {}").addArgument(ctx::getText).log();
        expressions.offer(new DeepFindByFieldNameJSONataFunction());
    }

    @Override
    public void exitToArray(ToArrayContext ctx) {
        logger.atInfo().setMessage("To array! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(currentFunction, new ArrayCastTransformerJSONataFunction()));
    }

    @Override
    public void exitArrayIndexQuery(ArrayIndexQueryContext ctx) {
        logger.atInfo().setMessage("Array index query! {}").addArgument(ctx::getText).log();
        if (expressions.isEmpty()) {
            expressions.offer(new ArrayIndexJSONataFunction(Integer.valueOf(ctx.NUMBER().getText())));
        } else {
            var previousFunction = expressions.removeLast();
            expressions.offer(new JoinJSONataFunction(previousFunction, new ArrayIndexJSONataFunction(Integer.valueOf(ctx.NUMBER().getText()))));
        }
    }

    @Override
    public void exitBooleanCompare(BooleanCompareContext ctx) {
        logger.atInfo().setMessage("Boolean compare! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new CompareValuesJSONataFunction(previousFunction, CompareOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitBooleanExpression(BooleanExpressionContext ctx) {
        logger.atInfo().setMessage("Boolean expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new BooleanExpressionJSONataFunction(previousFunction, BooleanOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitAlgebraicExpression(AlgebraicExpressionContext ctx) {
        logger.atInfo().setMessage("Algebraic expression! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new AlgebraicJSONataFunction(previousFunction, AlgebraicOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitInlineIfExpression(InlineIfExpressionContext ctx) {
        logger.atInfo().setMessage("Inline if expression! {}").addArgument(ctx::getText).log();
        var falseValueProvider = Optional.ofNullable(ctx.expression().size() == 3 ? expressions.removeLast() : null);
        var trueValueProvider = expressions.removeLast();
        var testProvider = expressions.removeLast();
        this.expressions.offer(new InlineIfJSONataFunction(testProvider, trueValueProvider, falseValueProvider));
    }

    @Override
    public void exitArrayQuery(ArrayQueryContext ctx) {
        logger.atInfo().setMessage("Array query! {}").addArgument(ctx::getText).log();
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new ArrayQueryJSONataFunction(previousFunction, currentFunction));
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
    public void exitRangeQuery(RangeQueryContext ctx) {
        logger.atInfo().setMessage("Range query! {}").addArgument(ctx::getText).log();
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

        var currentFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(currentFunction, new ArrayRangeJSONataFunction(startIndex, endIndex)));
    }

    @Override
    public void exitContextValue(ContextValueContext ctx) {
        logger.atInfo().setMessage("Context value! {}").addArgument(ctx::getText).log();
        var contextFunction = expressions.removeLast();
        this.expressions.offer(new ContextValueJSONataFunction(contextFunction));
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
        expressions.offer(new StringConcatJSONataFunction(previousFunction, currentFunction));
    }

    @Override
    public void exitArrayConstructor(ArrayConstructorContext ctx) {
        logger.atInfo().setMessage("Array constructor! {}").addArgument(ctx::getText).log();
        var expresisonCounter = ctx.expressionList().expression().size();
        var fns = new ArrayList<JSONataFunction>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            fns.addFirst(expressions.removeLast());
        }
        expressions.offer(new ArrayConstructorJSONataFunction(fns));
    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        logger.atInfo().setMessage("Object mapper! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, new ObjectMapperJSONataFunction(fieldList)));
    }

    @Override
    public void exitObjectConstructor(ObjectConstructorContext ctx) {
        logger.atInfo().setMessage("Object constructor! {}").addArgument(ctx::getText).log();
        var fieldList = objectFields(ctx.fieldList());
        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, new ObjectBuilderJSONataFunction(fieldList)));
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

    @Override
    public void exitObjectBuilder(ObjectBuilderContext ctx) {
        logger.atInfo().setMessage("Object builder! {}").addArgument(ctx::getText).log();
        expressions.offer(new ObjectBuilderJSONataFunction(objectFields(ctx.fieldList())));
    }

    public List<JSONataFunction> getExpressions() {
        return expressions.stream().toList();
    }
}