package dev.vepo.jsonata.parser;

import static dev.vepo.jsonata.functions.json.JsonFactory.numberValue;
import static dev.vepo.jsonata.functions.json.JsonFactory.stringValue;
import static org.apache.commons.text.StringEscapeUtils.unescapeJson;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import dev.vepo.jsonata.functions.ArrayCastTransformerJSONataFunction;
import dev.vepo.jsonata.functions.ArrayIndexJSONataFunction;
import dev.vepo.jsonata.functions.ArrayQueryJSONataFunction;
import dev.vepo.jsonata.functions.ArrayRangeJSONataFunction;
import dev.vepo.jsonata.functions.BooleanCompareJSONataFunction;
import dev.vepo.jsonata.functions.BooleanOperator;
import dev.vepo.jsonata.functions.CompareOperator;
import dev.vepo.jsonata.functions.CompareValuesJSONataFunction;
import dev.vepo.jsonata.functions.DeepFindByFieldNameJSONataFunction;
import dev.vepo.jsonata.functions.FieldPathJSONataFunction;
import dev.vepo.jsonata.functions.ContextValueJSONataFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.JoinJSONataFunction;
import dev.vepo.jsonata.functions.WildcardJSONataFunction;
import dev.vepo.jsonata.functions.generated.JSONataGrammarBaseListener;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.AllDescendantSearchContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayIndexQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ArrayQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.BooleanCompareContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ContextRefereceContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ContextValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ExpNumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FieldValuesContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.FloatValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.IdentifierContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.NumberValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.PathContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.RangeQueryContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.StringValueContext;
import dev.vepo.jsonata.functions.generated.JSONataGrammarParser.ToArrayContext;

public class JSONataGrammarListener extends JSONataGrammarBaseListener {
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

    // private static Function<Data, Data> toFunction(List<FieldNameContext> ctx) {
    //     var transform = new FieldPathJSONataFunction(ctx.stream()
    //                                                     .map(JSONataGrammarListener::fieldName2Text)
    //                                                     .toList());

    //     return value -> transform.map(value, value);
    // }

    // private static Function<Data, Data> toFunction(FieldPathOrStringContext ctx) {
    //     if (nonNull(ctx.STRING())) {
    //         return value -> stringValue(sanitise(ctx.STRING().getText()));
    //     } else if (nonNull(ctx.NUMBER())) {
    //         return value -> numberValue(Integer.valueOf(ctx.NUMBER().getText()));
    //     } else if(nonNull(ctx.FLOAT())) {
    //         return value -> numberValue(Double.valueOf(ctx.FLOAT().getText()));
    //     } else if (nonNull(ctx.EXP_NUMBER())) {
    //         return value -> numberValue(Float.valueOf(ctx.EXP_NUMBER().getText()));
    //     } else if (nonNull(ctx.BOOLEAN())) {
    //         return value -> booleanValue(Boolean.valueOf(ctx.BOOLEAN().getText()));
    //     } else if (nonNull(ctx.objectExpression())) {
    //         var mapper = toObjectMapper(ctx.objectExpression());
    //         return data -> mapper.map(data, data);
    //     } else {
    //         return toFunction(ctx.fieldPath().fieldName());
    //     }
    // }

    // private static Function<Data, Data> toValueProvider(StringOrFieldContext sCtx) {
    //     if (nonNull(sCtx.STRING())) {
    //         return value -> stringValue(sanitise(sCtx.getText()));
    //     } else if (nonNull(sCtx.NUMBER())) {
    //         return value -> stringValue(Integer.valueOf(sCtx.NUMBER().getText()).toString());
    //     } else if (nonNull(sCtx.BOOLEAN())) {
    //         return value -> stringValue(sCtx.BOOLEAN().getText());
    //     } else {
    //         var transform = new FieldPathJSONataFunction(sCtx.fieldPath()
    //                                                          .fieldName()
    //                                                          .stream()
    //                                                          .map(JSONataGrammarListener::fieldName2Text)
    //                                                          .toList());
    //         return value -> transform.map(value, value);
    //     }
    // }

    private final Deque<JSONataFunction> expressions;
    // private final Deque<DeclaredFunction> functionsDeclared;

    public JSONataGrammarListener() {
        this.expressions = new LinkedList<>();
        // this.expressions.offerFirst(new ArrayList<>()); // root
        // this.functionsDeclared = new LinkedList<>();
    }

    // @Override
    // public void enterFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
    //     this.expressions.offerFirst(new ArrayList<>()); // new stack
    // }

    // @Override
    // public void exitFunctionDeclarationBuilder(FunctionDeclarationBuilderContext ctx) {
    //     this.functionsDeclared.offerFirst(new DeclaredFunction(ctx.IDENTIFIER()
    //                                                               .stream()
    //                                                               .map(TerminalNode::getText)
    //                                                               .toList(),
    //                                                            this.expressions.pollFirst()));
    // }

    // public enum BuiltInFunction {
    //     SORT("$sort"),
    //     SUM("$sum");

    //     private String name;

    //     BuiltInFunction(String name) {
    //         this.name = name;
    //     }

    //     public static BuiltInFunction get(String name) {
    //         return Stream.of(values())
    //                      .filter(n -> n.name.compareToIgnoreCase(name) == 0)
    //                      .findAny()
    //                      .orElseThrow(() -> new JSONataException(String.format("Unknown function!!! function=%s", name)));
    //     }
    // }

    // @Override
    // public void exitFunctionCall(FunctionCallContext ctx) {
    //     expressions.peekFirst()
    //                .add(switch (BuiltInFunction.get(ctx.functionStatement().IDENTIFIER().getText())) {
    //                    case SORT -> new BuiltInSortJSONataFunction(new FieldPathJSONataFunction(ctx.functionStatement()
    //                                                                                                .parameterStatement()
    //                                                                                                .get(0)
    //                                                                                                .fieldPath()
    //                                                                                                .fieldName()
    //                                                                                                .stream()
    //                                                                                                .map(JSONataGrammarListener::fieldName2Text)
    //                                                                                                .toList()),
    //                                                                Optional.ofNullable(functionsDeclared.peekFirst()));
    //                    case SUM -> new BuiltInSumJSONataFunction(new FieldPathJSONataFunction(ctx.functionStatement()
    //                                                                                               .parameterStatement()
    //                                                                                               .get(0)
    //                                                                                               .fieldPath()
    //                                                                                               .fieldName()
    //                                                                                               .stream()
    //                                                                                               .map(JSONataGrammarListener::fieldName2Text)
    //                                                                                               .toList()));
    //                });
    // }

    // @Override
    // public void exitRootPath(RootPathContext ctx) {
    //     expressions.peekFirst()
    //                .add((original, value) -> original);
    // }

    @Override
    public void exitIdentifier(IdentifierContext ctx) {
        expressions.offer(new FieldPathJSONataFunction(Collections.singletonList(fieldName2Text(ctx.IDENTIFIER()))));

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
            expressions.offer(new ArrayCastTransformerJSONataFunction());
    }

    @Override
    public void exitArrayIndexQuery(ArrayIndexQueryContext ctx) {
            expressions.offer(new ArrayIndexJSONataFunction(Integer.valueOf(ctx.NUMBER().getText())));
    }
    
    @Override
    public void exitBooleanCompare(BooleanCompareContext ctx) {        
        var currentFunction = expressions.removeLast();
        this.expressions.offer(new CompareValuesJSONataFunction(CompareOperator.get(ctx.op.getText()), currentFunction));
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
        expressions.offer(new ArrayRangeJSONataFunction(startIndex, endIndex));
    }

    @Override
    public void exitContextValue(ContextValueContext ctx) {
        var contextFunction = expressions.removeLast();
        this.expressions.offer(new ContextValueJSONataFunction(contextFunction));
    }

    @Override
    public void exitContextReferece(ContextRefereceContext ctx) {
        this.expressions.offer((original, value) -> original);
    }

    // @Override
    // public void exitTransformerStringConcat(TransformerStringConcatContext ctx) {
    //     this.expressions.peekFirst()
    //                     .add(new StringConcatJSONataFunction(ctx.stringConcat()
    //                                                             .stringOrField()
    //                                                             .stream()
    //                                                             .map(JSONataGrammarListener::toValueProvider)
    //                                                             .toList()));

    // }

    // private static ObjectMapperJSONataFunction toObjectMapper(ObjectExpressionContext ctx) {
    //     return new ObjectMapperJSONataFunction(IntStream.range(0, ctx.fieldPathOrString().size() / 2)
    //                                                     .map(i -> i * 2)
    //                                                     .mapToObj(index -> new FieldContent(toFunction(ctx.fieldPathOrString(index)),
    //                                                                                         toFunction(ctx
    //                                                                                                       .fieldPathOrString(index + 1)),
    //                                                                                         nonNull(ctx.ARRAY_CAST(index))))
    //                                                     .toList());
    // }

    // @Override
    // public void exitObjectMapper(ObjectMapperContext ctx) {
    //     this.expressions.peekFirst()
    //                     .add(toObjectMapper(ctx.objectExpression()));
    // }

    // @Override
    // public void exitObjectBuilder(ObjectBuilderContext ctx) {
    //     this.expressions.peekFirst()
    //                     .add(new ObjectBuilderJSONataFunction(IntStream.range(0, ctx.objectExpression().fieldPathOrString().size() / 2)
    //                                                                    .map(i -> i * 2)
    //                                                                    .mapToObj(index -> new FieldContent(toFunction(ctx.objectExpression()
    //                                                                                                                      .fieldPathOrString(index)),
    //                                                                                                        toFunction(ctx.objectExpression()
    //                                                                                                                      .fieldPathOrString(index + 1)),
    //                                                                                                        nonNull(ctx.objectExpression()
    //                                                                                                                   .ARRAY_CAST(index))))
    //                                                                    .toList()));
    // }

    // @Override
    // public void exitArrayConstructorMapping(ArrayConstructorMappingContext ctx) {
    //     this.expressions.peekFirst()
    //                     .add(new ArrayConstructorJSONataFunction(ctx.arrayConstructor()
    //                                                                 .fieldPath()
    //                                                                 .stream()
    //                                                                 .map(fpCtx -> toFunction(fpCtx.fieldName()))
    //                                                                 .toList()));
    // }

    // @Override
    // public void exitTransformerWildcard(TransformerWildcardContext ctx) {
    //     expressions.peekFirst()
    //                .add(new WildcardJSONataFunction());
    // }

    // @Override
    // public void exitFieldPredicateArray(FieldPredicateArrayContext ctx) {
    //     expressions.peekFirst()
    //                .add(new FieldPredicateJSONataFunction(ctx.fieldPredicate().IDENTIFIER().getText(),
    //                                                       sanitise(ctx.fieldPredicate().STRING().getText())));
    // }

    // @Override
    // public void enterExpressionBooleanSentence(ExpressionBooleanSentenceContext ctx) {
    //     this.expressions.offerFirst(new ArrayList<>()); // new stack
    // }

    // @Override
    // public void enterExpressions(ExpressionsContext ctx) {
    //     if (ctx.parent instanceof ConditionalContext) {
    //         System.out.println("Conditional!!!!");
    //     }
    // }

    // @Override
    // public void exitConditional(ConditionalContext ctx) {
    //     // expressions.peek()
    //     System.out.println("Conditional!");
    // }

    // @Override
    // public void enterExpressionBooleanPredicate(ExpressionBooleanPredicateContext ctx) {
    //     this.expressions.offerFirst(new ArrayList<>()); // new stack
    // }

    // @Override
    // public void exitExpressionBooleanPredicate(ExpressionBooleanPredicateContext ctx) {
    //     var rightExpressions = this.expressions.pollFirst();
    //     this.expressions.peekFirst()
    //                     .add(new CompareValuesJSONataFunction(CompareOperator.get(ctx.booleanCompare().op.getText()), rightExpressions));
    // }

    // @Override
    // public void enterAlgebraicExpression(AlgebraicExpressionContext ctx) {
    //     this.expressions.offerFirst(new ArrayList<>()); // new stack
    // }

    // @Override
    // public void exitAlgebraicExpression(AlgebraicExpressionContext ctx) {
    //     var rightExpressions = this.expressions.pollFirst();
    //     this.expressions.peekFirst()
    //                     .add(new AlgebraicJSONataFunction(AlgebraicOperator.get(ctx.op.getText()), rightExpressions));
    // }

    public List<JSONataFunction> getExpressions() {
        return expressions.stream().toList();
    }
}