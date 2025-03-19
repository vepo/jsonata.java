package dev.vepo.jsonata;

import static dev.vepo.jsonata.TestData.INVOICE;
import static dev.vepo.jsonata.TestData.OBJECT;
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
        // Address.City => "Winchester"
        assertThat(jsonata("Address.City").evaluate(OBJECT).asText()).isEqualTo("Winchester");
        // // Phone.number => [ "0203 544 1234", "01962 001234", "01962 001235", "077 7700
        // // 1234" ]
        assertThat(jsonata("Phone.number").evaluate(OBJECT).multi().asText()).containsExactly("0203 544 1234", "01962 001234", "01962 001235", "077 7700 1234");
        // // Account.Order.Product.(Price * Quantity) => [ 68.9, 21.67, 137.8, 107.99 ]
        assertThat(jsonata("Account.Order.Product.(Price * Quantity)").evaluate(INVOICE).multi().asDouble()).containsExactly(68.9, 21.67, 137.8, 107.99);
        // Account.Order.OrderID.$uppercase() => [ "ORDER103", "ORDER104"]
        assertThat(jsonata("Account.Order.OrderID.$uppercase()").evaluate(INVOICE).multi().asText()).containsExactly("ORDER103", "ORDER104");
    }

    // @Test5
}
