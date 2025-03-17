package dev.vepo.jsonata;

import static dev.vepo.jsonata.JSONata.jsonata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

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
            assertThat(jsonata("Address[[0..3]]").evaluate(OBJECT)
                                                 .asText()).isEqualTo("{\"Street\":\"Hursley Park\",\"City\":\"Winchester\",\"Postcode\":\"SO21 2JN\"}");
            assertThat(jsonata("Address[[1..3]]").evaluate(OBJECT).isEmpty()).isTrue();
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
            assertThat(jsonata("Numbers[4] > Numbers[2]").evaluate(NUMBERS).asBoolean()).isTrue();
            assertThat(jsonata("Numbers[2] < Numbers[4]").evaluate(NUMBERS).asBoolean()).isTrue();
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
            assertThat(jsonata("Email.address").evaluate(OBJECT).multi().asText()).containsExactly("fred.smith@my-work.com",
                                                                                                   "fsmith@my-work.com",
                                                                                                   "freddy@my-social.com",
                                                                                                   "frederic.smith@very-serious.com");
        }

        @Test
        void arrayCastTest() {
            assertThat(jsonata("Email.[address]").evaluate(OBJECT)
                                                 .asText()).isEqualTo("[[\"fred.smith@my-work.com\",\"fsmith@my-work.com\"],[\"freddy@my-social.com\",\"frederic.smith@very-serious.com\"]]");
            assertThat(jsonata("[Address, Other.`Alternative.Address`].City").evaluate(OBJECT).multi().asText()).containsExactly("Winchester", "London");
        }
    }

    @Nested
    class ObjectConstructor {

        @Test
        void groupTest() {
            assertThat(jsonata("Account.Order.Product{`Product Name`: Price}").evaluate(INVOICE)
                                                                              .asText()).isEqualTo("{\"Bowler Hat\":[34.45,34.45],\"Trilby hat\":21.67,\"Cloak\":107.99}");
            assertThat(jsonata("""
                               Account.Order.Product {
                                   `Product Name`: {"Price": Price, "Qty": Quantity}
                               }
                               """).evaluate(INVOICE)
                                   .asText()).isEqualTo("{\"Bowler Hat\":{\"Price\":[34.45,34.45],\"Qty\":[2,4]},\"Trilby hat\":{\"Price\":21.67,\"Qty\":1},\"Cloak\":{\"Price\":107.99,\"Qty\":1}}");
            assertThat(jsonata("""
                               Account.Order.Product {
                                `Product Name`: $.{"Price": Price, "Qty": Quantity}
                               }
                               """).evaluate(INVOICE)
                                   .asText()).isEqualTo("{\"Bowler Hat\":[{\"Price\":34.45,\"Qty\":2},{\"Price\":34.45,\"Qty\":4}],\"Trilby hat\":{\"Price\":21.67,\"Qty\":1},\"Cloak\":{\"Price\":107.99,\"Qty\":1}}");
            assertThat(jsonata("Account.Order.Product{`Product Name`: $.(Price*Quantity)}").evaluate(INVOICE)
                                                                                           .asText()).isEqualTo("{\"Bowler Hat\":[68.9,137.8],\"Trilby hat\":21.67,\"Cloak\":107.99}");
            assertThat(jsonata("Account.Order.Product{`Product Name`: $sum($.(Price*Quantity))}").evaluate(INVOICE)
                                                                                                 .asText()).isEqualTo("{\"Bowler Hat\":206.7,\"Trilby hat\":21.67,\"Cloak\":107.99}");
        }

        @Test
        void objectMapperTest() {
            assertThat(jsonata("""
                               Account.Order.Product.{
                                   `Product Name`: Price
                               }
                               """).evaluate(INVOICE)
                                   .asText()).isEqualTo("[{\"Bowler Hat\":34.45},{\"Trilby hat\":21.67},{\"Bowler Hat\":34.45},{\"Cloak\":107.99}]");
        }

        @Test
        void arrayOfObjectsTest() {
            assertThat(jsonata("Phone.{type: number}").evaluate(OBJECT).multi().asText()).containsExactly(
                                                                                                          "{\"home\":\"0203 544 1234\"}",
                                                                                                          "{\"office\":\"01962 001234\"}",
                                                                                                          "{\"office\":\"01962 001235\"}",
                                                                                                          "{\"mobile\":\"077 7700 1234\"}");
            assertThat(jsonata("Phone{type: number}").evaluate(OBJECT)
                                                     .asText()).isEqualTo("{\"home\":\"0203 544 1234\",\"office\":[\"01962 001234\",\"01962 001235\"],\"mobile\":\"077 7700 1234\"}");
            assertThat(jsonata("Phone{type: number[]}").evaluate(OBJECT)
                                                       .asText()).isEqualTo("{\"home\":[\"0203 544 1234\"],\"office\":[\"01962 001234\",\"01962 001235\"],\"mobile\":[\"077 7700 1234\"]}");
            assertThat(jsonata("""
                               {
                                   'name': FirstName,
                                   'age': Age,
                                   "city": Address.City
                               }
                               """).evaluate("""
                                             {
                                                 "FirstName": "Fred",
                                                 "Surname": "Smith",
                                                 "Age": 28,
                                                 "Address": {
                                                    "City": "Winchester",
                                                    "Postcode": "SO21 2JN",
                                                    "Country": "UK",
                                                    "Street": "Hursley Park"
                                                 }
                                             }
                                             """).asText()).isEqualTo("{\"name\":\"Fred\",\"age\":28,\"city\":\"Winchester\"}");
        }
    }

    @Nested
    class Literal {
        @Test
        void literalTest() {
            assertThat(jsonata("""
                                   {
                                       "string": "Hello World!",
                                       "integer": 3,
                                       "float": 2.4,
                                       "binary": 175e-2,
                                       "boolean": true,
                                       "null": null
                                   }
                               """).evaluate("{}")
                                   .asText()).isEqualTo("{\"string\":\"Hello World!\",\"integer\":3,\"float\":2.4,\"binary\":1.75,\"boolean\":true,\"null\":null}");
        }
    }

    @Nested
    class Functions {
        @Test
        void stringTest() {
            assertThat(jsonata("$string(5)").evaluate("{}").asText()).isEqualTo("5");
            assertThat(jsonata("$length(\"0123456789\")").evaluate("{}").asText()).isEqualTo("10");
        }

        @Test
        void substringTest() {
            assertThat(jsonata("$substring(\"abcdef\",2)").evaluate("{}").asText()).isEqualTo("cdef");
            assertThat(jsonata("$substring(\"abcdef\",2,4)").evaluate("{}").asText()).isEqualTo("cd");
            assertThat(jsonata("$substringBefore(\"abcdef\", \"c\")").evaluate("{}").asText()).isEqualTo("ab");
            assertThat(jsonata("$substringAfter(\"abcdef\", \"c\")").evaluate("{}").asText()).isEqualTo("def");
        }

        @Test
        void stringCaseTest() {
            assertThat(jsonata("$uppercase(\"abcdef\")").evaluate("{}").asText()).isEqualTo("ABCDEF");
            assertThat(jsonata("$lowercase(\"ABCDEF\")").evaluate("{}").asText()).isEqualTo("abcdef");
        }

        @Test
        void stringTrimTest() {
            assertThat(jsonata("$trim(\"   abcdef\t\")").evaluate("{}").asText()).isEqualTo("abcdef");
        }

        @Test
        void sortTest() {
            assertThat(jsonata("""
                               $sort(Account.Order.Product, function($l, $r) {
                                   $l.Description.Weight > $r.Description.Weight
                               })
                               """).evaluate(ACCOUNT)
                                   .asText()).isEqualTo("{\"Name\":\"Chair\",\"Description\":{\"Weight\":2}}, {\"Name\":\"Table\",\"Description\":{\"Weight\":100}}, {\"Name\":\"House\",\"Description\":{\"Weight\":1200000}}, {\"Name\":\"City\",\"Description\":{\"Weight\":200000000000000000}}");
            assertThat(jsonata("""
                               $sort(Account.Order.Product, function($l, $r) {
                                   $l.Description.Weight < $r.Description.Weight
                               })
                               """).evaluate(ACCOUNT)
                                   .asText()).isEqualTo("{\"Name\":\"City\",\"Description\":{\"Weight\":200000000000000000}}, {\"Name\":\"House\",\"Description\":{\"Weight\":1200000}}, {\"Name\":\"Table\",\"Description\":{\"Weight\":100}}, {\"Name\":\"Chair\",\"Description\":{\"Weight\":2}}");
            assertThat(jsonata("""
                               $sort(Account.Order.Product, function($l, $r) {
                                   $l.Description.Weight < $r.Description.Weight
                               }).Name
                               """).evaluate(ACCOUNT).multi().asText()).containsExactly("City", "House", "Table", "Chair");
            var numbers = """
                          {
                              "Numbers": [5, 3, 1, 2, 4]
                          }
                          """;
            assertThat(jsonata("$sort(Numbers)").evaluate(numbers).multi().asInt()).containsExactly(1, 2, 3, 4, 5);
            var strings = """
                          {
                              "Strings": ["e", "c", "a", "b", "d"]
                          }
                          """;
            assertThat(jsonata("$sort(Strings)").evaluate(strings).multi().asText()).containsExactly("a", "b", "c", "d", "e");
            assertThat(jsonata("$sort(FirstName)").evaluate(OBJECT).multi().asText()).containsExactly("Fred");
        }

        @Test
        void sumTest() {
            assertThat(jsonata("$sum(Numbers)").evaluate(NUMBERS).asDouble()).isEqualTo(67.8);
            assertThat(jsonata("$sum(Numbers)").evaluate(NUMBERS).asInt()).isEqualTo(67);
            assertThat(jsonata("$sum(Numbers)").evaluate("{}").isEmpty()).isTrue();
            assertThat(jsonata("$sum(Numbers)").evaluate("{\"Numbers\": []}").asInt()).isZero();
            assertThat(jsonata("$sum(Account.Order.Product.Price)").evaluate(INVOICE).asDouble()).isEqualTo(198.56);
            assertThat(jsonata("$sum(Account.Order.Product.(Price*Quantity))").evaluate(INVOICE).asDouble()).isEqualTo(336.36);
        }
    }

    @Nested
    class Numbers {
        @Test
        void operationsTest() {
            assertThat(jsonata("5 + 5").evaluate("{}").asInt()).isEqualTo(10);
            assertThat(jsonata("5 - 5").evaluate("{}").asInt()).isZero();
            assertThat(jsonata("5 * 5").evaluate("{}").asInt()).isEqualTo(25);
            assertThat(jsonata("5 / 5").evaluate("{}").asInt()).isOne();
            assertThat(jsonata("5 % 5").evaluate("{}").asInt()).isZero();
            assertThat(jsonata("5 ^ 5").evaluate("{}").asInt()).isEqualTo(3125);
            assertThat(jsonata("Numbers[0] + Numbers[1]").evaluate(NUMBERS).asDouble()).isEqualTo(3.4);
            assertThat(jsonata("Numbers[0] - Numbers[4]").evaluate(NUMBERS).asDouble()).isEqualTo(-19.9);
            assertThat(jsonata("Numbers[0] * Numbers[5]").evaluate(NUMBERS).asDouble()).isEqualTo(30);
            assertThat(jsonata("Numbers[0] / Numbers[4]").evaluate(NUMBERS).asDouble()).isEqualTo(0.04784688995215, offset(0.0001));
            assertThat(jsonata("Numbers[2] % Numbers[5]").evaluate(NUMBERS).asDouble()).isEqualTo(3.5);

        }

        @Test
        void expressionTest() {
            assertThat(jsonata("(5 + 3) * 4").evaluate("{}").asInt()).isEqualTo(32);
            assertThat(jsonata("Age * 2").evaluate("""
                                                   {
                                                       "FirstName": "Fred",
                                                       "Surname": "Smith",
                                                       "Age": 28,
                                                       "Address": {
                                                           "Street": "Hursley Park",
                                                           "City": "Winchester",
                                                           "Postcode": "SO21 2JN"
                                                       }
                                                   }
                                                    """).asInt()).isEqualTo(56);
            assertThat(jsonata("Product.(Price * Quantity)").evaluate("""
                                                                      {
                                                                           "Product": [
                                                                             {
                                                                                  "Name": "City",
                                                                                  "Price": 100,
                                                                                  "Quantity": 2
                                                                             }, {
                                                                                  "Name": "Table",
                                                                                  "Price": 50,
                                                                                  "Quantity": 4
                                                                             }, {
                                                                                  "Name": "Chair",
                                                                                  "Price": 10,
                                                                                  "Quantity": 10
                                                                             }
                                                                           ]
                                                                      }
                                                                      """).multi().asInt()).containsExactly(200, 200, 100);
            assertThat(jsonata("Product.(Price * 5)").evaluate("""
                                                               {
                                                                    "Product": [
                                                                      {
                                                                           "Name": "City",
                                                                           "Price": 100,
                                                                           "Quantity": 2
                                                                      }, {
                                                                           "Name": "Table",
                                                                           "Price": 50,
                                                                           "Quantity": 4
                                                                      }, {
                                                                           "Name": "Chair",
                                                                           "Price": 10,
                                                                           "Quantity": 10
                                                                      }
                                                                    ]
                                                               }
                                                               """).multi().asInt()).containsExactly(500, 250, 50);

            assertThat(jsonata("Product.(7 * Price)").evaluate("""
                                                               {
                                                                    "Product": [
                                                                      {
                                                                           "Name": "City",
                                                                           "Price": 100,
                                                                           "Quantity": 2
                                                                      }, {
                                                                           "Name": "Table",
                                                                           "Price": 50,
                                                                           "Quantity": 4
                                                                      }, {
                                                                           "Name": "Chair",
                                                                           "Price": 10,
                                                                           "Quantity": 10
                                                                      }
                                                                    ]
                                                               }
                                                               """).multi().asInt()).containsExactly(700, 350, 70);

        }
    }

    @Nested
    class Programming {
        @Test
        void commentsDefitionTest() {
            assertThat(jsonata("""
                               /* This is a comment */
                               5 + 5
                               """).evaluate("{}").asInt()).isEqualTo(10);
            assertThat(jsonata("""
                               /* This is a comment
                                * with multiple lines
                                */
                               5 + 5
                               """).evaluate("{}").asInt()).isEqualTo(10);
        }

        // @Disabled
        @Test
        void variableDefinitionTest() {
            assertThat(jsonata("""
                               (
                                 $volume := function($l, $w, $h){ $l * $w * $h };
                                 $volume(10, 10, 5);
                               )
                               """).evaluate("{}").asInt()).isEqualTo(500);
            assertThat(jsonata("""
                               (
                                 $volume := function($l, $w, $h){ $l * $w * $h };
                                 $v1 := 10;
                                 $x2 := 5;
                                 $abcdefghijlmnopqrstuvxz := 10000;
                                 $volume($v1, $x2, $abcdefghijlmnopqrstuvxz);
                               )
                               """).evaluate("{}").asInt()).isEqualTo(500_000);
            assertThat(jsonata("""
                               (
                                   $x := 50;
                                   $fn := function($a) { $a + x};
                                   $fn($x);
                               )
                               """).evaluate("{\"x\":54}").asInt()).isEqualTo(104);
        }

        @Test
        void conditionalInlineTest() {
            assertThat(jsonata("""
                               Account.Order.Product.{
                                   `Product Name`: $.Price > 100 ? "Premium" : "Basic"
                               }
                               """).evaluate(INVOICE)
                                   .asText()).isEqualTo("[{\"Bowler Hat\":\"Basic\"},{\"Trilby hat\":\"Basic\"},{\"Bowler Hat\":\"Basic\"},{\"Cloak\":\"Premium\"}]");
        }
    }

    @Nested
    class JSONSupplier {
        @Test
        void jsonSupplierTest() {
            assertThat(jsonata("""
                               {
                                   "FirstName": "Fred",
                                   "Surname": "Smith",
                                   "Age": 28,
                                   "Address": {
                                       "Street": "Hursley Park",
                                       "City": "Winchester",
                                       "Postcode": "SO21 2JN"
                                   }
                               }
                               """).evaluate("{}")
                                   .asText()).isEqualTo("{\"FirstName\":\"Fred\",\"Surname\":\"Smith\",\"Age\":28,\"Address\":{\"Street\":\"Hursley Park\",\"City\":\"Winchester\",\"Postcode\":\"SO21 2JN\"}}");
        }
    }

    private static final String INVOICE = """
                                          {
                                            "Account": {
                                              "Account Name": "Firefly",
                                              "Order": [
                                                {
                                                  "OrderID": "order103",
                                                  "Product": [
                                                    {
                                                      "Product Name": "Bowler Hat",
                                                      "ProductID": 858383,
                                                      "SKU": "0406654608",
                                                      "Description": {
                                                        "Colour": "Purple",
                                                        "Width": 300,
                                                        "Height": 200,
                                                        "Depth": 210,
                                                        "Weight": 0.75
                                                      },
                                                      "Price": 34.45,
                                                      "Quantity": 2
                                                    },
                                                    {
                                                      "Product Name": "Trilby hat",
                                                      "ProductID": 858236,
                                                      "SKU": "0406634348",
                                                      "Description": {
                                                        "Colour": "Orange",
                                                        "Width": 300,
                                                        "Height": 200,
                                                        "Depth": 210,
                                                        "Weight": 0.6
                                                      },
                                                      "Price": 21.67,
                                                      "Quantity": 1
                                                    }
                                                  ]
                                                },
                                                {
                                                  "OrderID": "order104",
                                                  "Product": [
                                                    {
                                                      "Product Name": "Bowler Hat",
                                                      "ProductID": 858383,
                                                      "SKU": "040657863",
                                                      "Description": {
                                                        "Colour": "Purple",
                                                        "Width": 300,
                                                        "Height": 200,
                                                        "Depth": 210,
                                                        "Weight": 0.75
                                                      },
                                                      "Price": 34.45,
                                                      "Quantity": 4
                                                    },
                                                    {
                                                      "ProductID": 345664,
                                                      "SKU": "0406654603",
                                                      "Product Name": "Cloak",
                                                      "Description": {
                                                        "Colour": "Black",
                                                        "Width": 30,
                                                        "Height": 20,
                                                        "Depth": 210,
                                                        "Weight": 2
                                                      },
                                                      "Price": 107.99,
                                                      "Quantity": 1
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                          }
                                          """;

    private static final String ACCOUNT = """
                                          {
                                              "Account": {
                                                  "Order": {
                                                      "Product": [
                                                          {
                                                              "Name": "City",
                                                              "Description": {
                                                                  "Weight": 200000000000000000
                                                              }
                                                          }, {
                                                              "Name": "Table",
                                                              "Description": {
                                                                  "Weight": 100
                                                              }
                                                          }, {
                                                              "Name": "Chair",
                                                              "Description": {
                                                                  "Weight": 2
                                                              }
                                                          }, {
                                                              "Name": "House",
                                                              "Description": {
                                                                  "Weight": 1200000
                                                              }
                                                          }
                                                      ]
                                                  }
                                              }
                                          }
                                          """;

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
