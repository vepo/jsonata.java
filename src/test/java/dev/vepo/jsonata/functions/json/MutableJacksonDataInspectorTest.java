package dev.vepo.jsonata.functions.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;

class MutableJacksonDataInspectorTest {

  private static final MutableJacksonDataInspector INSPECTOR = MutableJacksonDataInspector.INSTANCE;

  private static final Data SAMPLE = Data.load("""
      {
        "items": [
          {"name": "first", "remove": true},
          {"name": "second", "remove": true}
        ],
        "label": "keep"
      }
      """);

  @Nested
  class Copy {

    @Test
    void createsIndependentDeepCopy() {
      var copy = INSPECTOR.copy(SAMPLE);

      assertThat(copy.toJson().toString()).isEqualTo(SAMPLE.toJson().toString());
      assertThat(copy.toJson()).isNotSameAs(SAMPLE.toJson());
      assertThat(copy.inspector()).isSameAs(MutableJacksonDataInspector.INSTANCE);
    }

    @Test
    void returnsEmptyForEmptyData() {
      assertThat(INSPECTOR.copy(dev.vepo.jsonata.functions.Mapping.empty()).isEmpty()).isTrue();
    }
  }

  @Nested
  class MergeFields {

    @Test
    void mergesTopLevelFieldsIntoMutableObject() {
      var copy = INSPECTOR.copy(SAMPLE);
      var target = copy.get("items").at(0);

      INSPECTOR.mergeFields(target, Data.load("{\"updated\": true, \"score\": 0}"));

      assertThat(target.get("updated").toJson().asBoolean()).isTrue();
      assertThat(target.get("score").toJson().asInt()).isZero();
      assertThat(target.get("name").toJson().asText()).isEqualTo("first");
    }

    @Test
    void throwsWhenTargetIsNotMutableObject() {
      assertThatThrownBy(() -> INSPECTOR.mergeFields(SAMPLE.get("label"), Data.load("{\"x\": 1}")))
          .isInstanceOf(JSONataException.class)
          .hasMessage("Transform update target must be an object");
    }
  }

  @Nested
  class RemoveFields {

    @Test
    void removesNamedFieldsFromMutableObject() {
      var copy = INSPECTOR.copy(SAMPLE);
      var target = copy.get("items").at(0);

      INSPECTOR.removeFields(target, java.util.List.of("remove", "missing"));

      assertThat(target.hasField("remove")).isFalse();
      assertThat(target.hasField("name")).isTrue();
    }

    @Test
    void isNoOpWhenTargetIsNotMutableObject() {
      var label = INSPECTOR.copy(SAMPLE).get("label");

      INSPECTOR.removeFields(label, java.util.List.of("label"));

      assertThat(label.toJson().asText()).isEqualTo("keep");
    }
  }

  @Nested
  class IsMutableObject {

    @Test
    void returnsTrueForObjectNodes() {
      assertThat(INSPECTOR.isMutableObject(SAMPLE.get("items").at(0))).isTrue();
    }

    @Test
    void returnsFalseForScalarNodes() {
      assertThat(INSPECTOR.isMutableObject(SAMPLE.get("label"))).isFalse();
    }
  }

  @Nested
  class FunctionalDefaults {

    @Test
    void mergedMutatesInPlaceAndReturnsTarget() {
      var copy = INSPECTOR.copy(SAMPLE);
      var target = copy.get("items").at(0);

      var result = INSPECTOR.merged(target, Data.load("{\"flag\": true}"));

      assertThat(result).isSameAs(target);
      assertThat(target.get("flag").toJson().asBoolean()).isTrue();
    }

    @Test
    void replaceNodeReplacesRoot() {
      var replacement = Data.load("{\"replaced\": true}");
      var result = INSPECTOR.replaceNode(SAMPLE, SAMPLE, replacement);

      assertThat(result.get("replaced").toJson().asBoolean()).isTrue();
      assertThat(SAMPLE.hasField("items")).isTrue();
    }
  }

  @Nested
  class MutableValues {

    @Test
    void isMutable() {
      assertThat(INSPECTOR.mutableValues()).isTrue();
    }
  }
}
