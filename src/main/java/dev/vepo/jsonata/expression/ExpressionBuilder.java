package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.numberValue;
import static dev.vepo.jsonata.expression.transformers.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import dev.vepo.jsonata.expression.Expression.ArrayCastTransformerExpression;
import dev.vepo.jsonata.expression.Expression.ArrayConstructorExpression;
import dev.vepo.jsonata.expression.Expression.ArrayIndexExpression;
import dev.vepo.jsonata.expression.Expression.ArrayRangeExpression;
import dev.vepo.jsonata.expression.Expression.BoleanExpression;
import dev.vepo.jsonata.expression.Expression.BooleanOperator;
import dev.vepo.jsonata.expression.Expression.CompareExpression;
import dev.vepo.jsonata.expression.Expression.CompareOperator;
import dev.vepo.jsonata.expression.Expression.DeepFindByFieldNameExpression;
import dev.vepo.jsonata.expression.Expression.FieldPathExpression;
import dev.vepo.jsonata.expression.Expression.FieldPredicateExpression;
import dev.vepo.jsonata.expression.Expression.InnerExpressions;
import dev.vepo.jsonata.expression.Expression.StringConcatExpression;
import dev.vepo.jsonata.expression.Expression.WildcardExpression;
import dev.vepo.jsonata.expression.generated.ExpressionsBaseListener;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.ArrayConstructorMappingContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.ExpressionBooleanPredicateContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.ExpressionBooleanSentenceContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.FieldNameContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.FieldPredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.IndexPredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.InnerExpressionContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.NumberValueContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.QueryPathContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.RangePredicateArrayContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.RootPathContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.StringOrFieldContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.StringValueContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.TransformerArrayCastContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.TransformerDeepFindByFieldContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.TransformerStringConcatContext;
import dev.vepo.jsonata.expression.generated.ExpressionsParser.TransformerWildcardContext;
import dev.vepo.jsonata.expression.transformers.Value;

public class ExpressionBuilder extends ExpressionsBaseListener {
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

    private final Deque<List<Expression>> expressions;

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
                   .add(new FieldPathExpression(ctx.fieldPath()
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
                        .add(new InnerExpressions(inner));
    }

    @Override
    public void exitTransformerStringConcat(TransformerStringConcatContext ctx) {
        this.expressions.peekFirst()
                        .add(new StringConcatExpression(ctx.stringConcat()
                                                           .stringOrField()
                                                           .stream()
                                                           .map(ExpressionBuilder::toValueProvider)
                                                           .toList()));

    }

    @Override
    public void exitArrayConstructorMapping(ArrayConstructorMappingContext ctx) {
        this.expressions.peekFirst()
                        .add(new ArrayConstructorExpression(ctx.arrayConstructor()
                                                               .fieldPath()
                                                               .stream()
                                                               .map(fpCtx -> toFunction(fpCtx.fieldName()))
                                                               .toList()));
    }

    @Override
    public void exitTransformerWildcard(TransformerWildcardContext ctx) {
        expressions.peekFirst()
                   .add(new WildcardExpression());
    }

    @Override
    public void exitTransformerDeepFindByField(TransformerDeepFindByFieldContext ctx) {
        expressions.peekFirst()
                   .add(new DeepFindByFieldNameExpression(ctx.fieldName().getText()));
    }

    @Override
    public void exitTransformerArrayCast(TransformerArrayCastContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayCastTransformerExpression());
    }

    @Override
    public void exitFieldPredicateArray(FieldPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new FieldPredicateExpression(ctx.fieldPredicate().IDENTIFIER().getText(),
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
                        .add(new BoleanExpression(BooleanOperator.get(ctx.booleanExpression().op.getText()), rightExpressions));
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
                   .add(new ArrayRangeExpression(startIndex, endIndex));
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
                        .add(new CompareExpression(CompareOperator.get(ctx.booleanCompare().op.getText()), rightExpressions));
    }

    @Override
    public void exitIndexPredicateArray(IndexPredicateArrayContext ctx) {
        expressions.peekFirst()
                   .add(new ArrayIndexExpression(Integer.valueOf(ctx.indexPredicate().NUMBER().getText())));
    }

    public List<Expression> getExpressions() {
        return expressions.peekFirst();
    }

    private static Function<Value, Value> toFunction(List<FieldNameContext> path) {
        var transform = new FieldPathExpression(path.stream()
                                                    .map(ExpressionBuilder::fieldName2Text)
                                                    .toList());
        return value -> transform.map(value, value);
    }

    private static Function<Value, Value> toValueProvider(StringOrFieldContext sCtx) {
        if (Objects.nonNull(sCtx.STRING())) {
            return value -> stringValue(sanitise(sCtx.getText()));
        } else if (Objects.nonNull(sCtx.NUMBER())) {
            return value -> stringValue(Integer.valueOf(sCtx.NUMBER().getText()).toString());
        } else if (Objects.nonNull(sCtx.BOOLEAN())) {
            return value -> stringValue(sCtx.BOOLEAN().getText());
        } else {
            var transform = new FieldPathExpression(sCtx.fieldPath()
                                                        .fieldName()
                                                        .stream()
                                                        .map(ExpressionBuilder::fieldName2Text)
                                                        .toList());
            return value -> transform.map(value, value);
        }
    }
}