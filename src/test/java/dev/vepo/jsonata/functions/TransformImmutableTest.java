package dev.vepo.jsonata.functions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.ImmutableJacksonDataInspector;

class TransformImmutableTest {

  private static final Data SAMPLE = Data.load("""
      {
        "items": [
          {"name": "first", "remove": true, "score": 1},
          {"name": "second", "remove": true, "score": 2}
        ],
        "label": "keep"
      }
      """);

  @Test
  void updatesResultWithoutMutatingOriginalInput() {
    var originalJson = SAMPLE.toJson().toString();
    var transform = new Transform(
        (original, current) -> current.get("items"),
        (original, match) -> Data.load("{\"updated\": true}"),
        Optional.of((original, match) -> Data.load("\"remove\"")));

    Data result = null;
    result = EvaluationContext.call(ImmutableJacksonDataInspector.INSTANCE,
            () -> transform.map(SAMPLE, SAMPLE));

    assertThat(SAMPLE.toJson().toString()).isEqualTo(originalJson);
    assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
    assertThat(result.get("items").at(0).hasField("remove")).isFalse();
    assertThat(result.get("items").at(0).get("score").toJson().asInt()).isEqualTo(1);
  }

  @Test
  void appliesUpdateToEachMatchedObject() {
    var transform = new Transform(
        (original, current) -> current.get("items"),
        (original, match) -> Data.load("{\"seen\": true}"),
        Optional.empty());

    var result = EvaluationContext.call(ImmutableJacksonDataInspector.INSTANCE,
            () -> transform.map(SAMPLE, SAMPLE));

    assertThat(result.get("items").at(0).get("seen").toJson().asBoolean()).isTrue();
    assertThat(result.get("items").at(1).get("seen").toJson().asBoolean()).isTrue();
  }
}
