package dev.vepo.jsonata;

import static dev.vepo.jsonata.JSONata.jsonata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.exception.JSONataException;

class JSONataTest {

    @Nested
    class SimpleQuery {
        @Test
        void queryTest() {
            assertThat(jsonata("Surname").evaluate(OBJECT).asText()).isEqualTo("Smith");
            assertThat(jsonata("Age").evaluate(OBJECT).asInt()).isEqualTo(28);
            assertThat(jsonata("Address.City").evaluate(OBJECT).asText()).isEqualTo("Winchester");
            assertThat(jsonata("Other.Misc").evaluate(OBJECT).isNull()).isTrue();
            assertThat(jsonata("Other.Nothing").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(jsonata("Other.`Over 18 ?`").evaluate(OBJECT).asBoolean()).isTrue();
        }

        @Test
        void notFoundPathTest() {
            assertThat(jsonata("Invalid").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(jsonata("Age.Invalid").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(jsonata("Invalid.Age").evaluate(OBJECT).isEmpty()).isTrue();
        }


        @Test
        void invalidJsonTest() {
            var mapping = jsonata("Invalid");
            assertThatThrownBy(() -> mapping.evaluate("{ssssss}")).isInstanceOf(JSONataException.class)
                    .hasMessage("Invalid JSON! content={ssssss}");
        }
    }

    @Nested
    class Validation {
        @Test
        void parseException() {
            assertThatThrownBy(() -> jsonata("")).isInstanceOf(ParseCancellationException.class);
        }

        @Test
        void arrayRangeCheckRuleValidation() {
            assertThatThrownBy(() -> jsonata("Phone[[-1..1]]")).hasMessage("Start index should be greather than 0!");
            assertThatThrownBy(() -> jsonata("Phone[[0..-1]]")).hasMessage("End index should be greather than 0!");
            assertThatThrownBy(() -> jsonata("Phone[[999..99]]")).hasMessage("End index should be greather than start index!");
        }
    }

    @Nested
    class NavigatingJsonArrays {
        @Test
        void arrayTest() {
            assertThat(jsonata("Phone[0]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"home\",\"number\":\"0203 544 1234\"}");
            assertThat(jsonata("Phone[1]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001234\"}");
            assertThat(jsonata("Phone[-1]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(jsonata("Phone[-2]").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001235\"}");
            assertThat(jsonata("Phone[8]").evaluate(OBJECT).isEmpty()).isTrue();
            assertThat(jsonata("Phone[0].number").evaluate(OBJECT).asText()).isEqualTo("0203 544 1234");
            assertThat(jsonata("Phone.number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234",
                                                                                                            "01962 001234",
                                                                                                            "01962 001235",
                                                                                                            "077 7700 1234");
            assertThat(jsonata("Phone.number[0]").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234",
                                                                                                               "01962 001234",
                                                                                                               "01962 001235",
                                                                                                               "077 7700 1234");
            assertThat(jsonata("(Phone.number)[0]").evaluate(OBJECT).asText()).isEqualTo("0203 544 1234");
            assertThat(jsonata("Phone[[0..1]]").evaluate(OBJECT).multi().asText()).containsExactly("{\"type\":\"home\",\"number\":\"0203 544 1234\"}",
                                                                                                             "{\"type\":\"office\",\"number\":\"01962 001234\"}");
        }
    }

    @Nested
    class Wildcard {
        @Test
        void wildCardTest() {
            assertThat(jsonata("Address.*").evaluate(OBJECT).multi().asText()).containsExactly("Hursley Park", "Winchester", "SO21 2JN");
            assertThat(jsonata("*.Postcode").evaluate(OBJECT).multi().asText()).containsExactly("SO21 2JN");
            assertThat(jsonata("**.Postcode").evaluate(OBJECT).multi().asText()).containsExactly("SO21 2JN", "E1 6RF");
            assertThat(jsonata("**.Postcode.*").evaluate(OBJECT).multi().asText()).containsExactly("SO21 2JN", "E1 6RF");
            assertThat(jsonata("**.InvalidField").evaluate(OBJECT).multi().asText()).isEmpty();
            assertThat(jsonata("**.InvalidField").evaluate("{}").multi().asText()).isEmpty();
            assertThat(jsonata("InvalidField.**.InvalidField").evaluate("{}").multi().asText()).isEmpty();

        }
    }

    @Nested
    class Predicates {
        @Test
        void queriesTest() {
            assertThat(jsonata("Phone[type='mobile']").evaluate(OBJECT).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(jsonata("Phone[type='mobile'].number").evaluate(OBJECT).asText()).isEqualTo("077 7700 1234");
            assertThat(jsonata("Phone[type='office'].number").evaluate(OBJECT).multi().asText()).containsExactly("01962 001234", "01962 001235");
        }

        @Test
        void arrayCastTest() {
            assertThat(jsonata("Address[].City").evaluate(OBJECT).multi().asText()).containsExactly("Winchester");
            assertThat(jsonata("Phone[0][].number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234");
            assertThat(jsonata("Phone[][type='home'].number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234");
            assertThat(jsonata("Phone[type='office'].number[]").evaluate(OBJECT).multi().asText()).containsExactly("01962 001234", "01962 001235");
        }
    }

    @Nested
    class Flattening {
        @Test
        void queriesTest() {
            assertThat(jsonata("$[0]").evaluate(ARRAY).asText()).isEqualTo("{\"ref\":[1,2]}");
            assertThat(jsonata("$[0].ref").evaluate(ARRAY).multi().asInt()).containsExactly(1, 2);
            assertThat(jsonata("$[0].ref[0]").evaluate(ARRAY).asInt()).isEqualTo(1);
            assertThat(jsonata("$.ref").evaluate(ARRAY).multi().asInt()).containsExactly(1, 2, 3, 4);
        }
    }

    @Nested
    class Expressions {
        @Test
        void stringTest() {
            assertThat(jsonata("FirstName & ' ' & Surname").evaluate(OBJECT).asText()).isEqualTo("Fred Smith");
            assertThat(jsonata("Address.(Street & ', ' & City)").evaluate(OBJECT).asText()).isEqualTo("Hursley Park, Winchester");
            assertThat(jsonata("5&0&true").evaluate(OBJECT).asText()).isEqualTo("50true");
        }

        @Test
        void numberTest() {
            assertThat(jsonata("Numbers[0] = Numbers[5]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(jsonata("Numbers[0] != Numbers[4]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(jsonata("Numbers[1] < Numbers[5]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(jsonata("Numbers[1] <= Numbers[5]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(jsonata("Numbers[2] > Numbers[4]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(jsonata("Numbers[2] >= Numbers[4]").evaluate(NUMBERS).asBoolean()).isFalse();
            assertThat(jsonata("\"01962 001234\" in Phone.number").evaluate(OBJECT).asBoolean()).isTrue();
        }

        @Test
        void booleanTest() {
            assertThat(jsonata("(Numbers[2] != 0) and (Numbers[5] != Numbers[1])").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(jsonata("(Numbers[2] != 0) or (Numbers[5] = Numbers[1])").evaluate(NUMBERS).asBoolean()).isTrue();
        }
    }

    @Nested
    class ArrayConstructor {
        @Test
        void arrayTest() {
            assertThat(jsonata("Email.address").evaluate(OBJECT).multi().asText()).containsExactly( "fred.smith@my-work.com",
                                                                                                   "fsmith@my-work.com",
                                                                                                   "freddy@my-social.com",
                                                                                                   "frederic.smith@very-serious.com");
        }

        @Test
        void arrayCastTest() {
            assertThat(jsonata("Email.[address]").evaluate(OBJECT).asText()).isEqualTo("[[\"fred.smith@my-work.com\",\"fsmith@my-work.com\"],[\"freddy@my-social.com\",\"frederic.smith@very-serious.com\"]]");
            assertThat(jsonata("[Address, Other.`Alternative.Address`].City").evaluate(OBJECT).multi().asText()).containsExactly("Winchester","London");
        }
    }

    @Nested
    class ObjectConstructor {
        @Test
        void arrayOfObjectsTest() {
            assertThat(jsonata("Phone.{type: number}").evaluate(OBJECT).multi().asText()).containsExactly(
                    "{\"home\":\"0203 544 1234\"}",
                    "{\"office\":\"01962 001234\"}",
                    "{\"office\":\"01962 001235\"}",
                    "{\"mobile\":\"077 7700 1234\"}");
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
