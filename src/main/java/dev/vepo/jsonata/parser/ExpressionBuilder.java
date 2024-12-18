package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

import dev.vepo.jsonata.functions.ArrayCastTransformerJSONFunction;
import dev.vepo.jsonata.functions.ArrayConstructorJSONFunction;
import dev.vepo.jsonata.functions.ArrayIndexJSONFunction;
import dev.vepo.jsonata.functions.ArrayRangeJSONFunction;
import dev.vepo.jsonata.functions.BooleanCompareJSONFunction;
import dev.vepo.jsonata.functions.BooleanOperator;
import dev.vepo.jsonata.functions.CompareOperator;
import dev.vepo.jsonata.functions.CompareValuesJSONFunction;
import dev.vepo.jsonata.functions.DeepFindByFieldNameJSONFunction;
import dev.vepo.jsonata.functions.FieldContent;
import dev.vepo.jsonata.functions.FieldPathJSONFunction;
import dev.vepo.jsonata.functions.FieldPredicateJSONFunction;
import dev.vepo.jsonata.functions.InnerFunctionJSONFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.ObjectMapperJSONataFunction;
import dev.vepo.jsonata.functions.StringConcatJSONFunction;
import dev.vepo.jsonata.functions.WildcardJSONFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.generated.JSONataGrammarBaseListener;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayConstructorMappingContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpressionBooleanPredicateContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpressionBooleanSentenceContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldNameContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldPredicateArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.IndexPredicateArrayContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.InnerExpressionContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.NumberValueContext;
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

public class ExpressionBuilder extends JSONataGrammarBaseListener {
    private static String fieldName2Text(FieldNameContext ctx) {
        if (Objects.nonNull(ctx.IDENTIFIER())) {
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

    private final Deque<List<JSONataFunction>> expressions;

    public ExpressionBuilder() {
        this.expressions = new LinkedList<>();
        this.expressions.offerFirst(new ArrayList<>()); // root
    }

    @Override
    public void exitRootPath(RootPathContext ctx) {
        expressions.peekFirst()
                   .add((original, value) -> original);
    }

    @Override
    public void exitQueryPath(QueryPathContext ctx) {
        expressions.peekFirst()
                   .add(new FieldPathJSONFunction(ctx.fieldPath()
                                                   .fieldName()
                                                   .stream()
                                                   .map(ExpressionBuilder::fieldName2Text)
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
                        .add(new InnerFunctionJSONFunction(inner));
    }

    @Override
    public void exitTransformerStringConcat(TransformerStringConcatContext ctx) {
        this.expressions.peekFirst()
                        .add(new StringConcatJSONFunction(ctx.stringConcat()
                                                           .stringOrField()
                                                           .stream()
                                                           .map(ExpressionBuilder::toValueProvider)
                                                           .toList()));

    }

    @Override
    public void exitObjectMapper(ObjectMapperContext ctx) {
        this.expressions.peekFirst()
                        .add(new ObjectMapperJSONataFunction(IntStream.range(0, ctx.objectExpression().fieldPath().size() / 2)
                                                                 .mapToObj(index -> new FieldContent(toFunction(ctx.objectExpression().fieldPath(index).fieldName()),
                                                                                                     toFunction(ctx.objectExpression().fieldPath(index + 1).fieldName())))
                                                                 .toList()));
    }

    @Override
    public void exitArrayConstructorMapping(ArrayConstructorMappingContext ctx) {
        this.expressions.peekFirst()
                        .add(new ArrayConstructorJSONFunction(ctx.arrayConstructor()
                                                               .fieldPath()
                                                               .stream()
                                                               .map(fpCtx -> toFunction(fpCtx.fieldName()))
                                                               .toList()));
    }

    @Override
    public void exitTransformerWildcard(TransformerWildcardContext ctx) {
        expressions.peekFirst()
                   .add(new WildcardJSONFunction());
    }

    @Override
    public void exitTransformerDeepFindByField(TransformerDeepFindByFieldContext ctx) {
        expressions.peekFirst()
                   .add(new DeepFindByFieldNameJSONFunction(ctx.fieldName().getText()));
    }

    @Override
    public void exitTransformerArrayCast(TransformerArrayCastContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayCastTransformerJSONFunction());
    }

    @Override
    public void exitFieldPredicateArray(FieldPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new FieldPredicateJSONFunction(ctx.fieldPredicate().IDENTIFIER().getText(),
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
                        .add(new BooleanCompareJSONFunction(BooleanOperator.get(ctx.booleanExpression().op.getText()), rightExpressions));
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
                   .add(new ArrayRangeJSONFunction(startIndex, endIndex));
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
                        .add(new CompareValuesJSONFunction(CompareOperator.get(ctx.booleanCompare().op.getText()), rightExpressions));
    }

    @Override
    public void exitIndexPredicateArray(IndexPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayIndexJSONFunction(Integer.valueOf(ctx.indexPredicate().NUMBER().getText())));
    }

    public List<JSONataFunction> getExpressions() {
        return expressions.peekFirst();
    }

    private static Function<Data, Data> toFunction(List<FieldNameContext> path) {
        var transform = new FieldPathJSONFunction(path.stream()
                                                    .map(ExpressionBuilder::fieldName2Text)
                                                    .toList());
        return value -> transform.map(value, value);
    }

    private static Function<Data, Data> toValueProvider(StringOrFieldContext sCtx) {
        if (Objects.nonNull(sCtx.STRING())) {
            return value -> stringValue(sanitise(sCtx.getText()));
        } else if (Objects.nonNull(sCtx.NUMBER())) {
            return value -> stringValue(Integer.valueOf(sCtx.NUMBER().getText()).toString());
        } else if (Objects.nonNull(sCtx.BOOLEAN())) {
            return value -> stringValue(sCtx.BOOLEAN().getText());
        } else {
            var transform = new FieldPathJSONFunction(sCtx.fieldPath()
                                                        .fieldName()
                                                        .stream()
                                                        .map(ExpressionBuilder::fieldName2Text)
                                                        .toList());
            return value -> transform.map(value, value);
        }
    }
}