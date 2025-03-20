package dev.vepo.jsonata;

import static dev.vepo.jsonata.TestData.INVOICE;
import static dev.vepo.jsonata.TestData.ADDRESS;
import static dev.vepo.jsonata.TestData.CUSTUMER;
import static dev.vepo.jsonata.JSONata.jsonata;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Path Operators
 * 
 * https://docs.jsonata.org/next/path-operators
 */
class OperatorsTest {
    @Test
    void pathTest() {
        assertThat(jsonata("Address.City").evaluate(ADDRESS).asText()).isEqualTo("Winchester");
        assertThat(jsonata("Phone.number").evaluate(ADDRESS).multi().asText()).containsExactly("0203 544 1234", "01962 001234", "01962 001235",
                                                                                               "077 7700 1234");
        assertThat(jsonata("Account.Order.Product.(Price * Quantity)").evaluate(INVOICE).multi().asDouble()).containsExactly(68.9, 21.67, 137.8, 107.99);
        assertThat(jsonata("Account.Order.OrderID.$uppercase()").evaluate(INVOICE).multi().asText()).containsExactly("ORDER103", "ORDER104");
    }

    @Test
    void rangeTest() {
        assertThat(jsonata("[1..5]").evaluate("{}").multi().asInt()).containsExactly(1, 2, 3, 4, 5);
        assertThat(jsonata("[1..3, 7..9]").evaluate("{}").multi().asInt()).containsExactly(1, 2, 3, 7, 8, 9);
        assertThat(jsonata("[1..$count(Items)].(\"Item \" & $)").evaluate("{\"Items\": [7,7,9]}").multi().asText()).containsExactly("Item 1", "Item 2",
                                                                                                                                    "Item 3");
    }

    @Test
    void chainTest() {
        assertThat(jsonata("Account.`Account Name` ~> $uppercase()").evaluate(INVOICE).asText()).isEqualTo("FIREFLY");
        assertThat(jsonata("Customer.Email ~> $substringAfter('@') ~> $substringBefore('.') ~> $uppercase()").evaluate(CUSTUMER).asText()).isEqualTo("VEPO");
        assertThat(jsonata("""
                           (
                              $normalize := $uppercase ~> $trim;
                              $normalize("   Some   Words   ")
                           )
                           """).evaluate("{}").asText()).isEqualTo("SOME   WORDS");
    }
}
