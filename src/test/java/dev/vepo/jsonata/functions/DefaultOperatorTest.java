package dev.vepo.jsonata.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.JSONata;

class DefaultOperatorTest {

    @Nested
    class DefaultOperatorCases {

        @Test
        void trueLiteralReturnsLeft() {
            assertThat(JSONata.jsonata("true ?: 42").evaluate("{}").asText()).isEqualTo("true");
        }

        @Test
        void falseLiteralReturnsRight() {
            assertThat(JSONata.jsonata("false ?: 42").evaluate("{}").asInt()).isEqualTo(42);
        }

        @Test
        void emptyObjectUsesRight() {
            assertThat(JSONata.jsonata("{} ?: 42").evaluate("{}").asInt()).isEqualTo(42);
        }

        @Test
        void objectWithPropertyUsesLeft() {
            assertThat(JSONata.jsonata("{ \"a\": 1 } ?: 42").evaluate("{}").asText()).isEqualTo("{\"a\":1}");
        }

        @Test
        void lambdaIifeSquare() {
            assertThat(JSONata.jsonata("function($x){$x*$x}(5)").evaluate("{}").asDouble()).isEqualTo(25);
        }
    }
}
