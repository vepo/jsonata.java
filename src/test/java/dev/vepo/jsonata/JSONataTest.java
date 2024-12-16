package dev.vepo.jsonata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JSONataTest {

    @Nested
    class SimpleQuery {
        @Test
        void queryTest() {
            assertThat(JSONata.of("Surname").evaluate(OBJECT).asText()).isEqualTo("Smith");
            assertThat(JSONata.of("Age").evaluate(OBJECT).asInt()).isEqualTo(28);
            assertThat(JSONata.of("Address.City").evaluate(OBJECT).asText()).isEqualTo("Winchester");
            assertThat(JSONata.of("Other.Misc").evaluate(OBJECT).isNull()).isTrue();
            assertThat(JSONata.of("Other.Nothing").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(JSONata.of("Other.`Over 18 ?`").evaluate(OBJECT).asBoolean()).isTrue();
        }
    }

    @Nested
    class Validation {
        @Test
        void parseException() {
            assertThatThrownBy(() -> JSONata.of("")).isInstanceOf(ParseCancellationException.class);
        }

        @Test
        void arrayRangeCheckRuleValidation() {
            assertThatThrownBy(() -> JSONata.of("Phone[[-1..1]]")).hasMessage("Start index should be greather than 0!");
            assertThatThrownBy(() -> JSONata.of("Phone[[0..-1]]")).hasMessage("End index should be greather than 0!");
            assertThatThrownBy(() -> JSONata.of("Phone[[999..99]]")).hasMessage("End index should be greather than start index!");
        }
    }

    @Nested
    class NavigatingJsonArrays {
        @Test
        void arrayTest() {
            assertThat(JSONata.of("Phone[0]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"home\",\"number\":\"0203 544 1234\"}");
            assertThat(JSONata.of("Phone[1]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001234\"}");
            assertThat(JSONata.of("Phone[-1]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(JSONata.of("Phone[-2]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001235\"}");
            assertThat(JSONata.of("Phone[8]").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(JSONata.of("Phone[0].number").evaluate(OBJECT).asText()).isEqualTo("0203 544 1234");
            assertThat(JSONata.of("Phone.number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234",
                                                                                                            "01962 001234",
                                                                                                            "01962 001235",
                                                                                                            "077 7700 1234");
            assertThat(JSONata.of("Phone.number[0]").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234",
                                                                                                               "01962 001234",
                                                                                                               "01962 001235",
                                                                                                               "077 7700 1234");
            assertThat(JSONata.of("(Phone.number)[0]").evaluate(OBJECT).asText()).isEqualTo("0203 544 1234");
            assertThat(JSONata.of("Phone[[0..1]]").evaluate(OBJECT).multi().asText()).containsExactly("{\"type\":\"home\",\"number\":\"0203 544 1234\"}",
                                                                                                             "{\"type\":\"office\",\"number\":\"01962 001234\"}");
        }
    }

    @Nested
    class Wildcard {
        @Test
        void wildCardTest() {
            assertThat(JSONata.of("Address.*").evaluate(OBJECT).multi().asText()).containsExactly("Hursley Park", "Winchester", "SO21 2JN");
            assertThat(JSONata.of("*.Postcode").evaluate(OBJECT).multi().asText()).containsExactly("SO21 2JN");
        }
    }

    @Nested
    class Predicates {
        @Test
        void queriesTest() {
            assertThat(JSONata.of("Phone[type='mobile']").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(JSONata.of("Phone[type='mobile'].number").evaluate(OBJECT).asText()).isEqualTo("077 7700 1234");
            assertThat(JSONata.of("Phone[type='office'].number").evaluate(OBJECT).multi().asText()).containsExactly("01962 001234", "01962 001235");
        }

        @Test
        void arrayCastTest() {
            assertThat(JSONata.of("Address[].City").evaluate(OBJECT).multi().asText()).containsExactly("Winchester");
            assertThat(JSONata.of("Phone[0][].number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234");
            assertThat(JSONata.of("Phone[][type='home'].number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234");
            assertThat(JSONata.of("Phone[type='office'].number[]").evaluate(OBJECT).multi().asText()).containsExactly("01962 001234", "01962 001235");
        }
    }

    @Nested
    class Flattening {
        @Test
        void queriesTest() {
            assertThat(JSONata.of("$[0]").evaluate(ARRAY).asText()).isEqualTo("{\"ref\":[1,2]}");
            assertThat(JSONata.of("$[0].ref").evaluate(ARRAY).multi().asInt()).containsExactly(1, 2);
            assertThat(JSONata.of("$[0].ref[0]").evaluate(ARRAY).asInt()).isEqualTo(1);
            assertThat(JSONata.of("$.ref").evaluate(ARRAY).multi().asInt()).containsExactly(1, 2, 3, 4);
        }
    }

    @Nested
    class Expressions {
        @Test
        void stringTest() {
            assertThat(JSONata.of("FirstName & ' ' & Surname").evaluate(OBJECT).asText()).isEqualTo("Fred Smith");
            assertThat(JSONata.of("Address.(Street & ', ' & City)").evaluate(OBJECT).asText()).isEqualTo("Hursley Park, Winchester");
            assertThat(JSONata.of("5&0&true").evaluate(OBJECT).asText()).isEqualTo("50true");
        }

        @Test
        void numberTest() {
            assertThat(JSONata.of("Numbers[0] = Numbers[5]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(JSONata.of("Numbers[0] != Numbers[4]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(JSONata.of("Numbers[1] < Numbers[5]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(JSONata.of("Numbers[1] <= Numbers[5]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(JSONata.of("Numbers[2] > Numbers[4]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(JSONata.of("Numbers[2] >= Numbers[4]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(JSONata.of("\"01962 001234\" in Phone.number").evaluate(OBJECT).asBoolean()).isTrue();
        }
    }

    private static final String NUMBERS = """
                                                 {
                                                   "Numbers": [1, 2.4, 3.5, 10, 20.9, 30]
                                                 }
                                                 """;

    private static final String ARRAY = """
                                               [
                                                 { "ref": [ 1,2 ] },
                                                 { "ref": [ 3,4 ] }
                                               ]
                                                 """;

    private static final String OBJECT = """
                                                {
                                                    "FirstName": "Fred",
                                                    "Surname": "Smith",
                                                    "Age": 28,
                                                    "Address": {
                                                        "Street": "Hursley Park",
                                                        "City": "Winchester",
                                                        "Postcode": "SO21 2JN"
                                                    },
                                                    "Phone": [
                                                        {
                                                             "type": "home",
                                                             "number": "0203 544 1234"
                                                        },
                                                        {
                                                             "type": "office",
                                                             "number": "01962 001234"
                                                        },
                                                        {
                                                             "type": "office",
                                                             "number": "01962 001235"
                                                        },
                                                        {
                                                             "type": "mobile",
                                                             "number": "077 7700 1234"
                                                        }
                                                    ],
                                                    "Email": [
                                                        {
                                                             "type": "work",
                                                             "address": ["fred.smith@my-work.com", "fsmith@my-work.com"]
                                                        },
                                                        {
                                                             "type": "home",
                                                             "address": ["freddy@my-social.com", "frederic.smith@very-serious.com"]
                                                        }
                                                    ],
                                                    "Other": {
                                                        "Over 18 ?": true,
                                                        "Misc": null,
                                                        "Alternative.Address": {
                                                        "Street": "Brick Lane",
                                                        "City": "London",
                                                        "Postcode": "E1 6RF"
                                                        }
                                                    }
                                                 }
                                                """;
}
