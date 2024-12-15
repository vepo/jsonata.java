package dev.vepo.jsonata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JSONataTest {

    String content = """
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
            assertThat(JSONata.of("Surname").evaluate(content).asText()).isEqualTo("Smith");
            assertThat(JSONata.of("Age").evaluate(content).asInt()).isEqualTo(28);
            assertThat(JSONata.of("Address.City").evaluate(content).asText()).isEqualTo("Winchester");
            assertThat(JSONata.of("Other.Misc").evaluate(content).isNull()).isTrue();
            assertThat(JSONata.of("Other.Nothing").evaluate(content).isEmpty()).isTrue();
            assertThat(JSONata.of("Other.`Over 18 ?`").evaluate(content).asBoolean()).isTrue();
        }
    }

    @Nested
    class NavigatingJsonArrays{ 
        @Test
        void arrayTests() {
            assertThat(JSONata.of("Phone[0]").evaluate(content).asText()).isEqualTo("{\"type\":\"home\",\"number\":\"0203 544 1234\"}");
            assertThat(JSONata.of("Phone[1]").evaluate(content).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001234\"}");
            assertThat(JSONata.of("Phone[-1]").evaluate(content).asText()).isEqualTo("{\"type\":\"mobile\",\"number\":\"077 7700 1234\"}");
            assertThat(JSONata.of("Phone[-2]").evaluate(content).asText()).isEqualTo("{\"type\":\"office\",\"number\":\"01962 001235\"}");
            assertThat(JSONata.of("Phone[8]").evaluate(content).isEmpty()).isTrue();
            assertThat(JSONata.of("Phone[0].number").evaluate(content).asText()).isEqualTo("0203 544 1234");
            assertThat(JSONata.of("Phone.number").evaluate(content).multi().asText()).containsExactly("0203 544 1234", "01962 001234", "01962 001235", "077 7700 1234");
            assertThat(JSONata.of("Phone.number[0]").evaluate(content).multi().asText()).containsExactly("0203 544 1234", "01962 001234", "01962 001235", "077 7700 1234");
            assertThat(JSONata.of("(Phone.number)[0]").evaluate(content).asText()).isEqualTo("0203 544 1234");
        }
    }
}
