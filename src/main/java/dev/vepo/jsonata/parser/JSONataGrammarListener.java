package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static java.util.Objects.nonNull;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.antlr.v4.runtime.tree.TerminalNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.ArrayCastTransformerJSONataFunction;
import dev.vepo.jsonata.functions.ArrayConstructorJSONataFunction;
import dev.vepo.jsonata.functions.ArrayIndexJSONataFunction;
import dev.vepo.jsonata.functions.ArrayRangeJSONataFunction;
import dev.vepo.jsonata.functions.BooleanCompareJSONataFunction;
import dev.vepo.jsonata.functions.BooleanOperator;
import dev.vepo.jsonata.functions.BuiltInSortJSONataFunction;
import dev.vepo.jsonata.functions.CompareOperator;
import dev.vepo.jsonata.functions.CompareValuesJSONataFunction;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.DeepFindByFieldNameJSONataFunction;
import dev.vepo.jsonata.functions.FieldContent;
import dev.vepo.jsonata.functions.FieldPathJSONataFunction;
import dev.vepo.jsonata.functions.FieldPredicateJSONataFunction;
import dev.vepo.jsonata.functions.InnerFunctionJSONataFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.ObjectBuilderJSONataFunction;
import dev.vepo.jsonata.functions.ObjectMapperJSONataFunction;
import dev.vepo.jsonata.functions.StringConcatJSONataFunction;
import dev.vepo.jsonata.functions.WildcardJSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.generated.JSONataGrammarBaseListener;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayConstructorMappingContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpressionBooleanPredicateContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpressionBooleanSentenceContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldNameContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldPathOrStringContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldPredicateArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FunctionCallContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FunctionDeclarationBuilderContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.IndexPredicateArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.InnerExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.NumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectBuilderContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ObjectMapperContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.QueryPathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RangePredicateArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RootPathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.StringOrFieldContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.StringValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.TransformerArrayCastContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.TransformerDeepFindByFieldContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.TransformerStringConcatContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.TransformerWildcardContext;

public class JSONataGrammarListener extends JSONataGrammarBaseListener {
    private static String fieldName2Text(FieldNameContext ctx) {
        if (nonNull(ctx.IDENTIFIER())) {
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

    private static Function<Data, Data> toFunction(List<FieldNameContext> ctx) {
        var transform = new FieldPathJSONataFunction(ctx.stream()
                                                        .map(JSONataGrammarListener::fieldName2Text)
                                                        .toList());

        return value -> transform.map(value, value);
    }

    private static Function<Data, Data> toFunction(FieldPathOrStringContext ctx) {
        if (Objects.nonNull(ctx.STRING())) {
            return value -> stringValue(sanitise(ctx.STRING().getText()));
        } else {
            return toFunction(ctx.fieldPath().fieldName());
        }
    }

    private static Function<Data, Data> toValueProvider(StringOrFieldContext sCtx) {
        if (nonNull(sCtx.STRING())) {
            return value -> stringValue(sanitise(sCtx.getText()));
        } else if (nonNull(sCtx.NUMBER())) {
            return value -> stringValue(Integer.valueOf(sCtx.NUMBER().getText()).toString());
        } else if (nonNull(sCtx.BOOLEAN())) {
            return value -> stringValue(sCtx.BOOLEAN().getText());
        } else {
            var transform = new FieldPathJSONataFunction(sCtx.fieldPath()
                                                             .fieldName()
                                                             .stream()
                                                             .map(JSONataGrammarListener::fieldName2Text)
                                                             .toList());
            return value -> transform.map(value, value);
        }
    }

    private final Deque<List<JSONataFunction>> expressions;
    private final Deque<DeclaredFunction> functionsDeclared;

    public JSONataGrammarListener() {
        this.expressions = new LinkedList<>();
        this.expressions.offerFirst(new ArrayList<>()); // root
        this.functionsDeclared = new LinkedList<>();
    }

    @Override
    public void enterFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        this.expressions.offerFirst(new ArrayList<>()); // new stack
    }

    @Override
    public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
        this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.IDENTIFIER()
                                                                  .stream()
                                                                  .map(TerminalNode::getText)
                                                                  .toList(),
                                                               this.expressions.pollFirst()));
    }

    public enum BuiltInFunction {
        SORT("$sort");

        private String name;

        BuiltInFunction(String name) {
            this.name = name;
        }

        public static BuiltInFunction get(String name) {
            return Stream.of(values())
                         .filter(n -> n.name.compareToIgnoreCase(name) == 0)
                         .findAny()
                         .orElseThrow(() -> new JSONataException(String.format("Unknown function!!! function=%s", name)));
        }
    }

    @Override
    public void exitFunctionCall(FunctionCallContext ctx) {
        expressions.peekFirst()
                   .add(switch (BuiltInFunction.get(ctx.functionStatement().IDENTIFIER().getText())) {
                       case SORT -> new BuiltInSortJSONataFunction(new FieldPathJSONataFunction(ctx.functionStatement()
                                                                                                   .parameterStatement()
                                                                                                   .get(0)
                                                                                                   .fieldPath()
                                                                                                   .fieldName()
                                                                                                   .stream()
                                                                                                   .map(JSONataGrammarListener::fieldName2Text)
                                                                                                   .toList()),
                                                                   Optional.ofNullable(functionsDeclared.peekFirst()));
                   });
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        expressions.peekFirst()
                   .add((original, value) -> original);
    }

    @Override
    public void exitQueryPath(QueryPathContext ctx) {
        expressions.peekFirst()
                   .add(new FieldPathJSONataFunction(ctx.fieldPath()
                                                        .fieldName()
                                                        .stream()
                                                        .map(JSONataGrammarListener::fieldName2Text)
                                                        .toList()));
    }

    @Override
    public void enterInnerExpression(InnerExpressionContext ctx) {
        this.expressions.offerFirst(new ArrayList<>()); // new stack
    }

    @Override
    public void exitInnerExpression(InnerExpressionContext ctx) {
        var inner = this.expressions.pollFirst();
        this.expressions.peekFirst()
                        .add(new InnerFunctionJSONataFunction(inner));
    }

    @Override
    public void exitTransformerStringConcat(TransformerStringConcatContext ctx) {
        this.expressions.peekFirst()
                        .add(new StringConcatJSONataFunction(ctx.stringConcat()
                                                                .stringOrField()
                                                                .stream()
                                                                .map(JSONataGrammarListener::toValueProvider)
                                                                .toList()));

    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        this.expressions.peekFirst()
                        .add(new ObjectMapperJSONataFunction(IntStream.range(0, ctx.objectExpression().fieldPathOrString().size() / 2)
                                                                      .map(i -> i * 2)
                                                                      .mapToObj(index -> new FieldContent(toFunction(ctx.objectExpression()
                                                                                                                        .fieldPathOrString(index)),
                                                                                                          toFunction(ctx.objectExpression()
                                                                                                                        .fieldPathOrString(index + 1)),
                                                                                                          nonNull(ctx.objectExpression()
                                                                                                                     .ARRAY_CAST(index))))
                                                                      .toList()));
    }

    @Override
    public void exitObjectBuilder(ObjectBuilderContext ctx) {
        this.expressions.peekFirst()
                        .add(new ObjectBuilderJSONataFunction(IntStream.range(0, ctx.objectExpression().fieldPathOrString().size() / 2)
                                                                       .map(i -> i * 2)
                                                                       .mapToObj(index -> new FieldContent(toFunction(ctx.objectExpression()
                                                                                                                         .fieldPathOrString(index)),
                                                                                                           toFunction(ctx.objectExpression()
                                                                                                                         .fieldPathOrString(index + 1)),
                                                                                                           nonNull(ctx.objectExpression()
                                                                                                                      .ARRAY_CAST(index))))
                                                                       .toList()));
    }

    @Override
    public void exitArrayConstructorMapping(ArrayConstructorMappingContext ctx) {
        this.expressions.peekFirst()
                        .add(new ArrayConstructorJSONataFunction(ctx.arrayConstructor()
                                                                    .fieldPath()
                                                                    .stream()
                                                                    .map(fpCtx -> toFunction(fpCtx.fieldName()))
                                                                    .toList()));
    }

    @Override
    public void exitTransformerWildcard(TransformerWildcardContext ctx) {
        expressions.peekFirst()
                   .add(new WildcardJSONataFunction());
    }

    @Override
    public void exitTransformerDeepFindByField(TransformerDeepFindByFieldContext ctx) {
        expressions.peekFirst()
                   .add(new DeepFindByFieldNameJSONataFunction(ctx.fieldName().getText()));
    }

    @Override
    public void exitTransformerArrayCast(TransformerArrayCastContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayCastTransformerJSONataFunction());
    }

    @Override
    public void exitFieldPredicateArray(FieldPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new FieldPredicateJSONataFunction(ctx.fieldPredicate().IDENTIFIER().getText(),
                                                          sanitise(ctx.fieldPredicate().STRING().getText())));
    }

    @Override
    public void enterExpressionBooleanSentence(ExpressionBooleanSentenceContext ctx) {
        this.expressions.offerFirst(new ArrayList<>()); // new stack
    }

    @Override
    public void exitExpressionBooleanSentence(ExpressionBooleanSentenceContext ctx) {
        var rightExpressions = this.expressions.pollFirst();
        this.expressions.peekFirst()
                        .add(new BooleanCompareJSONataFunction(BooleanOperator.get(ctx.booleanExpression().op.getText()), rightExpressions));
    }

    @Override
    public void exitRangePredicateArray(RangePredicateArrayContext ctx) {
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
        expressions.peek()
                   .add(new ArrayRangeJSONataFunction(startIndex, endIndex));
    }

    @Override
    public void exitStringValue(StringValueContext ctx) {
        expressions.peekFirst()
                   .add((original, current) -> stringValue(sanitise(ctx.getText())));
    }

    @Override
    public void exitNumberValue(NumberValueContext ctx) {
        expressions.peekFirst()
                   .add((original, current) -> numberValue(Integer.valueOf(ctx.getText())));
    }

    @Override
    public void enterExpressionBooleanPredicate(ExpressionBooleanPredicateContext ctx) {
        this.expressions.offerFirst(new ArrayList<>()); // new stack
    }

    @Override
    public void exitExpressionBooleanPredicate(ExpressionBooleanPredicateContext ctx) {
        var rightExpressions = this.expressions.pollFirst();
        this.expressions.peekFirst()
                        .add(new CompareValuesJSONataFunction(CompareOperator.get(ctx.booleanCompare().op.getText()), rightExpressions));
    }

    @Override
    public void exitIndexPredicateArray(IndexPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayIndexJSONataFunction(Integer.valueOf(ctx.indexPredicate().NUMBER().getText())));
    }

    public List<JSONataFunction> getExpressions() {
        return expressions.peekFirst();
    }
}