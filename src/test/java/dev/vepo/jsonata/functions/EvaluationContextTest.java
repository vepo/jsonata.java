package dev.vepo.jsonata.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.functions.data.DataInspectors;
import dev.vepo.jsonata.functions.json.ImmutableJacksonDataInspector;
import dev.vepo.jsonata.functions.json.MutableJacksonDataInspector;

class EvaluationContextTest {

  @Test
  void exposesInspectorInsideRun() {
    EvaluationContext.run(ImmutableJacksonDataInspector.INSTANCE, () -> assertThat(EvaluationContext.currentInspector())
        .isSameAs(ImmutableJacksonDataInspector.INSTANCE));
  }

  @Test
  void restoresPreviousInspectorAfterRun() {
    EvaluationContext.run(ImmutableJacksonDataInspector.INSTANCE, () -> {
    });

    assertThat(EvaluationContext.currentInspector()).isSameAs(DataInspectors.defaultInspector());
  }

  @Test
  void callReturnsValue() {
    var value = EvaluationContext.call(MutableJacksonDataInspector.INSTANCE, () -> 42);

    assertThat(value).isEqualTo(42);
  }
}
