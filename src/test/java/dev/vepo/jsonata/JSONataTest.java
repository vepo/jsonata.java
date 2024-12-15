package dev.vepo.jsonata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JSONataTest {

    String arrayContent = """
                          [
                            { "ref": [ 1,2 ] },
                            { "ref": [ 3,4 ] }
                          ]
                            """;
    String objectContent = """
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

    @Nested
    class SimpleQuery {
        @Test
        void queryTests() {
            assertThat(JSONata.of("Surname").evaluate(objectContent).asText()).isEqualTo("Smith");
            assertThat(JSONata.of("Age").evaluate(objectContent).asInt()).isEqualTo(28);
            assertThat(JSONata.of("Address.City").evaluate(objectContent).asText()).isEqualTo("Winchester");
            assertThat(JSONata.of("Other.Misc").evaluate(objectContent).isNull()).isTrue();
            assertThat(JSONata.of("Other.Nothing").evaluate(objectContent).isEmpty()).isTrue();
            assertThat(JSONata.of("Other.`Over 18 ?`").evaluate(objectContent).asBoolean()).isTrue();
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
        void arrayTests() {
            assertThat(JSONata.of("Phone[0]").evaluate(objectContent).asText()).isEqualTo("{\"type\":\"home\",\"number\":\"0203 544 1234\"}");
            assertThat(JSONata.of("Phone[1]").evaluate(objectContent).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001234\"}");
            assertThat(JSONata.of("Phone[-1]").evaluate(objectContent).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(JSONata.of("Phone[-2]").evaluate(objectContent).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001235\"}");
            assertThat(JSONata.of("Phone[8]").evaluate(objectContent).isEmpty()).isTrue();
            assertThat(JSONata.of("Phone[0].number").evaluate(objectContent).asText()).isEqualTo("0203 544 1234");
            assertThat(JSONata.of("Phone.number").evaluate(objectContent).multi().asText()).containsExactly("0203 544 1234",
                                                                                                            "01962 001234",
                                                                                                            "01962 001235",
                                                                                                            "077 7700 1234");
            assertThat(JSONata.of("Phone.number[0]").evaluate(objectContent).multi().asText()).containsExactly("0203 544 1234",
                                                                                                               "01962 001234",
                                                                                                               "01962 001235",
                                                                                                               "077 7700 1234");
            assertThat(JSONata.of("(Phone.number)[0]").evaluate(objectContent).asText()).isEqualTo("0203 544 1234");
            assertThat(JSONata.of("Phone[[0..1]]").evaluate(objectContent).multi().asText()).containsExactly("{\"type\":\"home\",\"number\":\"0203 544 1234\"}",
                                                                                                             "{\"type\":\"office\",\"number\":\"01962 001234\"}");
        }
    }

    @Nested
    class Flattening {
        @Test
        void queriesTest() {
            assertThat(JSONata.of("$[0]").evaluate(arrayContent).asText()).isEqualTo("{\"ref\":[1,2]}");
            assertThat(JSONata.of("$[0].ref").evaluate(arrayContent).multi().asInt()).containsExactly(1, 2);
            assertThat(JSONata.of("$[0].ref[0]").evaluate(arrayContent).asInt()).isEqualTo(1);
            assertThat(JSONata.of("$.ref").evaluate(arrayContent).multi().asInt()).containsExactly(1, 2, 3, 4);
        }
    }
}
