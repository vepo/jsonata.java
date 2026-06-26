package dev.vepo.jsonata.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

class TransformTest {

    private static final Data SAMPLE = Data.load("""
            {
              "items": [
                {"name": "first", "remove": true, "score": 1},
                {"name": "second", "remove": true, "score": 2}
              ],
              "label": "keep",
              "untouched": {"value": 99}
            }
            """);

    @Nested
    class UndefinedTransformTarget {

        @Test
        void returnsEmptyWhenTargetIsEmpty() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"flag\": true}"),
                    Optional.empty());

            assertThat(transform.map(SAMPLE, Mapping.empty()).isEmpty()).isTrue();
        }

        @Test
        void returnsEmptyWhenTargetIsNull() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"flag\": true}"),
                    Optional.empty());

            assertThat(transform.map(SAMPLE, null).isEmpty()).isTrue();
        }
    }

    @Nested
    class UndefinedPatternMatch {

        @Test
        void returnsUnchangedDeepCopyWhenPatternMatchesNothing() {
            var transform = new Transform(
                    (original, current) -> Mapping.empty(),
                    (original, match) -> Data.load("{\"applied\": true}"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(json(result)).isEqualTo(json(SAMPLE));
            assertThat(result.toJson()).isNotSameAs(SAMPLE.toJson());
        }
    }

    @Nested
    class Update {

        @Test
        void mergesObjectUpdateIntoMatchedNode() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"updated\": true, \"score\": 0}"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
            assertThat(result.get("items").at(0).get("score").toJson().asInt()).isZero();
            assertThat(result.get("items").at(0).get("name").toJson().asText()).isEqualTo("first");
            assertThat(result.get("label").toJson().asText()).isEqualTo("keep");
        }

        @Test
        void appliesUpdateToSingleMatchedObject() {
            var transform = new Transform(
                    (original, current) -> current.get("untouched"),
                    (original, match) -> Data.load("{\"value\": 100}"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("untouched").get("value").toJson().asInt()).isEqualTo(100);
        }

        @Test
        void appliesUpdateToEachMatchedObjectInSequence() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"seen\": true}"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).get("seen").toJson().asBoolean()).isTrue();
            assertThat(result.get("items").at(1).get("seen").toJson().asBoolean()).isTrue();
        }

        @Test
        void skipsUndefinedUpdateValue() {
            var transform = itemsTransform(
                    (original, match) -> Mapping.empty(),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(json(result)).isEqualTo(json(SAMPLE));
        }

        @Test
        void skipsNonObjectUpdateValue() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("5"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).hasField("updated")).isFalse();
            assertThat(result.get("items").at(0).get("score").toJson().asInt()).isEqualTo(1);
        }

        @Test
        void throwsWhenUpdateTargetIsNotAnObject() {
            var transform = new Transform(
                    (original, current) -> current.get("label"),
                    (original, match) -> Data.load("{\"applied\": true}"),
                    Optional.empty());

            assertThatThrownBy(() -> transform.map(SAMPLE, SAMPLE))
                    .isInstanceOf(JSONataException.class)
                    .hasMessage("Transform update target must be an object");
        }
    }

    @Nested
    class Delete {

        @Test
        void removesSingleFieldWhenDeleteExpressionIsAString() {
            var transform = itemsTransform(
                    (original, match) -> Mapping.empty(),
                    Optional.of((original, match) -> JsonFactory.stringValue("remove")));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).hasField("remove")).isFalse();
            assertThat(result.get("items").at(0).hasField("name")).isTrue();
        }

        @Test
        void removesMultipleFieldsFromEachMatch() {
            var transform = itemsTransform(
                    (original, match) -> Mapping.empty(),
                    Optional.of((original, match) -> Data.load("[\"remove\", \"score\"]")));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).hasField("remove")).isFalse();
            assertThat(result.get("items").at(0).hasField("score")).isFalse();
            assertThat(result.get("items").at(0).get("name").toJson().asText()).isEqualTo("first");
            assertThat(result.get("items").at(1).hasField("remove")).isFalse();
            assertThat(result.get("items").at(1).hasField("score")).isFalse();
        }

        @Test
        void skipsUndefinedDeleteExpression() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"updated\": true}"),
                    Optional.of((original, match) -> Mapping.empty()));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
            assertThat(result.get("items").at(0).hasField("remove")).isTrue();
        }

        @Test
        void ignoresNonTextualDeleteNames() {
            var transform = itemsTransform(
                    (original, match) -> Mapping.empty(),
                    Optional.of((original, match) -> Data.load("[\"remove\", 5, true]")));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).hasField("remove")).isFalse();
            assertThat(result.get("items").at(0).hasField("score")).isTrue();
        }

        @Test
        void skipsDeleteWhenMatchIsNotAnObject() {
            var transform = new Transform(
                    (original, current) -> current.get("label"),
                    (original, match) -> Mapping.empty(),
                    Optional.of((original, match) -> JsonFactory.stringValue("label")));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("label").toJson().asText()).isEqualTo("keep");
        }

        @Test
        void doesNothingWhenDeleteMappingIsAbsent() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"updated\": true}"),
                    Optional.empty());

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).hasField("remove")).isTrue();
            assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
        }
    }

    @Nested
    class CombinedTransform {

        @Test
        void appliesUpdateAndDeleteInSamePass() {
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"updated\": true}"),
                    Optional.of((original, match) -> JsonFactory.stringValue("remove")));

            var result = transform.map(SAMPLE, SAMPLE);

            assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
            assertThat(result.get("items").at(0).hasField("remove")).isFalse();
            assertThat(result.get("items").at(0).get("score").toJson().asInt()).isEqualTo(1);
        }
    }

    @Nested
    class DeepCopy {

        @Test
        void doesNotMutateOriginalInput() {
            var originalJson = json(SAMPLE);
            var transform = itemsTransform(
                    (original, match) -> Data.load("{\"updated\": true}"),
                    Optional.of((original, match) -> JsonFactory.stringValue("remove")));

            transform.map(SAMPLE, SAMPLE);

            assertThat(json(SAMPLE)).isEqualTo(originalJson);
        }
    }

    private static Transform itemsTransform(Mapping update, Optional<Mapping> delete) {
        return new Transform((original, current) -> current.get("items"), update, delete);
    }

    private static String json(Data data) {
        return data.toJson().toString();
    }
}
