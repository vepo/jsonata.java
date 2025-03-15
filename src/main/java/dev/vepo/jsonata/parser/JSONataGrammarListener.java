package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.booleanValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.antlr.v4.runtime.tree.TerminalNode;

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
        // this.expressions.offerFirst(new ArrayList<>()); // root
        this.functionsDeclared = new LinkedList<>();
    }

    @Override
    public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.IDENTIFIER()
                                                                  .stream()
                                                                  .map(TerminalNode::getText)
                                                                  .toList(),
                                                               this.expressions.removeLast()));
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        var valueProvider = expressions.removeLast();
        Optional<DeclaredFunction> maybeFn = functionsDeclared.isEmpty() ? Optional.empty() : Optional.of(functionsDeclared.removeLast());
        expressions.offer(switch (BuiltInFunction.get(ctx.functionStatement().IDENTIFIER().getText())) {
            case SORT -> new BuiltInSortJSONataFunction(valueProvider, maybeFn);
            case SUM -> new BuiltInSumJSONataFunction(valueProvider);
        });
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        expressions.offer((original, value) -> value);
    }

    @Override
    public void exitIdentifier(IdentifierContext ctx) {
        expressions.offer(new FieldMapJSONataFunction(fieldName2Text(ctx.IDENTIFIER())));
    }

    @Override
    public void exitPath(PathContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, currentFunction));
    }

    @Override
    public void exitFieldValues(FieldValuesContext ctx) {
        expressions.offer(new WildcardJSONataFunction());
    }

    @Override
    public void exitAllDescendantSearch(AllDescendantSearchContext ctx) {
        expressions.offer(new DeepFindByFieldNameJSONataFunction());
    }

    @Override
    public void exitToArray(ToArrayContext ctx) {
        var currentFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(currentFunction, new ArrayCastTransformerJSONataFunction()));
    }

    @Override
    public void exitArrayIndexQuery(ArrayIndexQueryContext ctx) {
        if (expressions.isEmpty()) {
            expressions.offer(new ArrayIndexJSONataFunction(Integer.valueOf(ctx.NUMBER().getText())));
        } else {
            var previousFunction = expressions.removeLast();
            expressions.offer(new JoinJSONataFunction(previousFunction, new ArrayIndexJSONataFunction(Integer.valueOf(ctx.NUMBER().getText()))));
        }
    }

    @Override
    public void exitBooleanCompare(BooleanCompareContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new CompareValuesJSONataFunction(previousFunction, CompareOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitBooleanExpression(BooleanExpressionContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new BooleanExpressionJSONataFunction(previousFunction, BooleanOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitAlgebraicExpression(AlgebraicExpressionContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new AlgebraicJSONataFunction(previousFunction, AlgebraicOperator.get(ctx.op.getText()), currentFunction));
    }

    @Override
    public void exitInlineIfExpression(InlineIfExpressionContext ctx) {
        var falseValueProvider = Optional.ofNullable(ctx.expression().size() == 3 ? expressions.removeLast() : null);
        var trueValueProvider = expressions.removeLast();
        var testProvider = expressions.removeLast();
        this.expressions.offer(new InlineIfJSONataFunction(testProvider, trueValueProvider, falseValueProvider));
    }

    @Override
    public void exitArrayQuery(ArrayQueryContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        this.expressions.offer(new ArrayQueryJSONataFunction(previousFunction, currentFunction));
    }

    @Override
    public void exitStringValue(StringValueContext ctx) {
        expressions.offer((original, current) -> stringValue(sanitise(ctx.getText())));
    }

    @Override
    public void exitNumberValue(NumberValueContext ctx) {
        expressions.offer((original, current) -> numberValue(Integer.valueOf(ctx.getText())));
    }

    @Override
    public void exitFloatValue(FloatValueContext ctx) {
        expressions.offer((original, current) -> numberValue(Double.valueOf(ctx.getText())));
    }

    @Override
    public void exitExpNumberValue(ExpNumberValueContext ctx) {
        expressions.offer((original, current) -> numberValue(Float.valueOf(ctx.getText())));
    }

    @Override
    public void exitBooleanValue(BooleanValueContext ctx) {
        expressions.offer((original, current) -> booleanValue(Boolean.valueOf(ctx.getText())));
    }

    @Override
    public void exitRangeQuery(RangeQueryContext ctx) {
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
        var contextFunction = expressions.removeLast();
        this.expressions.offer(new ContextValueJSONataFunction(contextFunction));
    }

    @Override
    public void exitContextReferece(ContextRefereceContext ctx) {
        this.expressions.offer((original, value) -> value);
    }

    @Override
    public void exitConcatValues(ConcatValuesContext ctx) {
        var currentFunction = expressions.removeLast();
        var previousFunction = expressions.removeLast();
        expressions.offer(new StringConcatJSONataFunction(previousFunction, currentFunction));
    }

    @Override
    public void exitArrayConstructor(ArrayConstructorContext ctx) {
        var expresisonCounter = ctx.expressionList().expression().size();
        var fns = new ArrayList<JSONataFunction>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            fns.addFirst(expressions.removeLast());
        }
        expressions.offer(new ArrayConstructorJSONataFunction(fns));
    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        var expresisonCounter = ctx.fieldList().expression().size();
        var fieldBuilder = new ArrayList<FieldContent>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            var valueFn = expressions.removeLast();
            var fieldFn = expressions.removeLast();
            fieldBuilder.addFirst(new FieldContent(fieldFn, valueFn));
        }

        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, new ObjectMapperJSONataFunction(fieldBuilder)));
    }

    @Override
    public void exitObjectConstructor(ObjectConstructorContext ctx) {
        var expresisonCounter = ctx.fieldList().expression().size();
        var fieldBuilder = new ArrayList<FieldContent>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            var valueFn = expressions.removeLast();
            var fieldFn = expressions.removeLast();
            fieldBuilder.addFirst(new FieldContent(fieldFn, valueFn));
        }

        var previousFunction = expressions.removeLast();
        expressions.offer(new JoinJSONataFunction(previousFunction, new ObjectBuilderJSONataFunction(fieldBuilder)));
    }

    @Override
    public void exitObjectBuilder(ObjectBuilderContext ctx) {
        var expresisonCounter = ctx.fieldList().expression().size();
        var fieldBuilder = new ArrayList<FieldContent>(expresisonCounter);
        for (int i = 0; i < expresisonCounter; ++i) {
            var valueFn = expressions.removeLast();
            var fieldFn = expressions.removeLast();
            fieldBuilder.addFirst(new FieldContent(fieldFn, valueFn));
        }
        expressions.offer(new ObjectBuilderJSONataFunction(fieldBuilder));
    }

    public List<JSONataFunction> getExpressions() {
        return expressions.stream().toList();
    }
}