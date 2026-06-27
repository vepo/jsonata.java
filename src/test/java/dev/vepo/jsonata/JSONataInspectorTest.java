package dev.vepo.jsonata;

import static dev.vepo.jsonata.JSONata.jsonata;
import static dev.vepo.jsonata.TestData.ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.ImmutableJacksonDataInspector;
import dev.vepo.jsonata.functions.json.MutableJacksonDataInspector;

class JSONataInspectorTest {

  @Test
  void jsonataFactoryAcceptsInspector() {
    var expression = jsonata("Surname", ImmutableJacksonDataInspector.INSTANCE);

    assertThat(expression.environment().dataInspector()).isSameAs(ImmutableJacksonDataInspector.INSTANCE);
    assertThat(expression.evaluate(ADDRESS).asText()).isEqualTo("Smith");
  }

  @Test
  void evaluateTagsDataWithSessionInspector() {
    var expression = jsonata("$", ImmutableJacksonDataInspector.INSTANCE);

    expression.evaluate(ADDRESS);

    assertThat(expression.environment().dataInspector().mutableValues()).isFalse();
  }

  @Test
  void defaultUsesMutableInspector() {
  var expression = jsonata("Age");

    assertThat(expression.environment().dataInspector()).isSameAs(MutableJacksonDataInspector.INSTANCE);
    assertThat(expression.evaluate(ADDRESS).asInt()).isEqualTo(28);
  }

  @Test
  void environmentBuilderAcceptsInspector() {
    var env = EvaluationEnvironment.builder()
                                   .dataInspector(ImmutableJacksonDataInspector.INSTANCE)
                                   .build();

    assertThat(jsonata("FirstName", env).evaluate(ADDRESS).asText()).isEqualTo("Fred");
  }

  @Test
  void immutableInspectorRejectsDirectMutation() {
    var inspector = ImmutableJacksonDataInspector.INSTANCE;
    var data = Data.load("{\"a\": 1}");

    assertThatThrownBy(() -> inspector.mergeFields(data, Data.load("{\"b\": 2}")))
        .isInstanceOf(JSONataException.class)
        .hasMessage("Cannot mutate immutable data");
  }
}
