package dev.vepo.jsonata;

import static dev.vepo.jsonata.JSONata.jsonata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import dev.vepo.jsonata.functions.data.Data;

class EvaluationEnvironmentTest {

    @Nested
    class RegisterFunction {

        @ParameterizedTest
        @CsvSource({"greet", "$greet"})
        void normalizesFunctionName(String name) {
            var impl = constant("1");
            var env = EvaluationEnvironment.empty().registerFunction(name, impl);

            assertThat(env.functions()).containsKey("$greet");
            assertThat(env.functions().get("$greet")).isSameAs(impl);
        }

        @Test
        void returnsNewEnvironmentWithoutMutatingOriginal() {
            var original = EvaluationEnvironment.empty();
            var impl = constant("1");

            var updated = original.registerFunction("greet", impl);

            assertThat(original.functions()).isEmpty();
            assertThat(updated.functions()).containsKey("$greet");
            assertThat(updated).isNotSameAs(original);
        }

        @Test
        void preservesExistingBindings() {
            var value = JsonNodeFactory.instance.numberNode(7);
            var env = EvaluationEnvironment.empty()
                                           .bind("value", value)
                                           .registerFunction("double", call -> {
                                               var n = call.current().toJson().asInt();
                                               return Data.load(String.valueOf(n * 2));
                                           });

            assertThat(env.bindings()).containsEntry("value", value);
            assertThat(env.functions()).containsKey("$double");
        }

        @Test
        void chainsMultipleFunctions() {
            var greet = constant("1");
            var farewell = constant("2");

            var env = EvaluationEnvironment.empty()
                                           .registerFunction("greet", greet)
                                           .registerFunction("farewell", farewell);

            assertThat(env.functions()).containsOnlyKeys("$greet", "$farewell");
            assertThat(env.functions().get("$greet")).isSameAs(greet);
            assertThat(env.functions().get("$farewell")).isSameAs(farewell);
        }

        @Test
        void replacesFunctionWithSameName() {
            var first = constant("1");
            var second = constant("2");

            var env = EvaluationEnvironment.empty()
                                           .registerFunction("greet", first)
                                           .registerFunction("greet", second);

            assertThat(env.functions()).containsOnlyKeys("$greet");
            assertThat(env.functions().get("$greet")).isSameAs(second);
        }
    }

    @Nested
    class BuilderRegisterFunction {

        @Test
        void registersFunctionForEvaluation() {
            var env = EvaluationEnvironment.builder()
                                           .registerFunction("greet", constant("\"hello\""))
                                           .build();

            assertThat(jsonata("$greet()", env).evaluate("{}").asText()).isEqualTo("hello");
        }
    }

    @Nested
    class RegisteredFunctionEvaluation {

        @Test
        void invokesZeroArgumentFunction() {
            var env = EvaluationEnvironment.empty()
                                           .registerFunction("answer", constant("42"));

            assertThat(jsonata("$answer()", env).evaluate("{}").asInt()).isEqualTo(42);
        }

        @Test
        void passesEvaluatedArgumentsToImplementation() {
            var env = EvaluationEnvironment.empty()
                                           .registerFunction("add", call -> {
                                               var left = call.arguments().get(0).map(call.original(), call.current());
                                               var right = call.arguments().get(1).map(call.original(), call.current());
                                               var sum = left.toJson().asInt() + right.toJson().asInt();
                                               return Data.load(String.valueOf(sum));
                                           });

            assertThat(jsonata("$add(10, 32)", env).evaluate("{}").asInt()).isEqualTo(42);
        }

        @Test
        void exposesOriginalAndCurrentContext() {
            var originalRef = new AtomicReference<Data>();
            var currentRef = new AtomicReference<Data>();

            var env = EvaluationEnvironment.empty()
                                           .registerFunction("probe", call -> {
                                               originalRef.set(call.original());
                                               currentRef.set(call.current());
                                               return call.current();
                                           });

            var input = "{\"name\":\"Ada\"}";
            jsonata("$probe()", env).evaluate(input);

            assertThat(originalRef.get().toJson().get("name").asText()).isEqualTo("Ada");
            assertThat(currentRef.get()).isSameAs(originalRef.get());
        }

        @Test
        void requiresRegistrationBeforeParsing() {
            assertThatThrownBy(() -> jsonata("$missing()", EvaluationEnvironment.empty()))
                    .isInstanceOf(dev.vepo.jsonata.exception.JSONataException.class)
                    .hasMessage("Function not found: $missing");
        }

        @Test
        void registerFunctionAfterParseDoesNotAffectExistingExpression() {
            var originalImpl = addDoubled();
            var env = EvaluationEnvironment.empty().registerFunction("twice", originalImpl);
            var parsed = jsonata("$twice(21)", env);

            var replacement = constant("0");
            var altered = parsed.registerFunction("twice", replacement);

            assertThat(altered.evaluate("{}").asInt()).isEqualTo(42);
            assertThat(altered.environment().functions().get("$twice")).isSameAs(replacement);
        }
    }

    private static java.util.function.Function<EvaluationEnvironment.MappingCall, Data> constant(String json) {
        return call -> Data.load(json);
    }

    private static java.util.function.Function<EvaluationEnvironment.MappingCall, Data> addDoubled() {
        return call -> {
            var value = call.arguments().getFirst().map(call.original(), call.current());
            return Data.load(String.valueOf(value.toJson().asInt() * 2));
        };
    }
}
