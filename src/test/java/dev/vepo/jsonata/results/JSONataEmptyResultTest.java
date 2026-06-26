package dev.vepo.jsonata.results;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.vepo.jsonata.JSONataResult;

class JSONataEmptyResultTest {

    private static final int STRESS_ITERATIONS = 10_000;

    private JSONataResult empty;

    @BeforeEach
    void setUp() {
        empty = JSONataResults.empty();
    }

    @Nested
    class TypePredicates {

        @Test
        void reportsEmptyNotNullOrTyped() {
            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.isNull()).isFalse();
            assertThat(empty.isInt()).isFalse();
            assertThat(empty.isDouble()).isFalse();
        }

        @Test
        void stressTypePredicatesRemainStable() {
            for (var i = 0; i < STRESS_ITERATIONS; i++) {
                assertThat(empty.isEmpty()).isTrue();
                assertThat(empty.isNull()).isFalse();
                assertThat(empty.isInt()).isFalse();
                assertThat(empty.isDouble()).isFalse();
            }
        }
    }

    @Nested
    class ScalarAccess {

        static Stream<String> scalarMethods() {
            return Stream.of("asText", "asInt", "asDouble", "asBoolean");
        }

        @ParameterizedTest
        @MethodSource("scalarMethods")
        void rejectsScalarCoercion(String method) {
            assertThatThrownBy(() -> invoke(empty, method))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Value is empty");
        }

        @ParameterizedTest
        @MethodSource("scalarMethods")
        void stressScalarCoercionAlwaysFails(String method) {
            for (var i = 0; i < STRESS_ITERATIONS; i++) {
                assertThatThrownBy(() -> invoke(empty, method))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Value is empty");
            }
        }

        private static void invoke(JSONataResult result, String method) {
            switch (method) {
                case "asText" -> result.asText();
                case "asInt" -> result.asInt();
                case "asDouble" -> result.asDouble();
                case "asBoolean" -> result.asBoolean();
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            }
        }
    }

    @Nested
    class MultiValue {

        @Test
        void exposesEmptyListsForEveryType() {
            var multi = empty.multi();

            assertThat(multi.asText()).isEmpty();
            assertThat(multi.asInt()).isEmpty();
            assertThat(multi.asDouble()).isEmpty();
            assertThat(multi.asBoolean()).isEmpty();
        }

        @Test
        void stressMultiReturnsImmutableEmptyLists() {
            for (var i = 0; i < STRESS_ITERATIONS; i++) {
                var multi = empty.multi();

                assertThat(multi.asText()).isSameAs(emptyList());
                assertThat(multi.asInt()).isSameAs(emptyList());
                assertThat(multi.asDouble()).isSameAs(emptyList());
                assertThat(multi.asBoolean()).isSameAs(emptyList());
            }
        }

        @Test
        void stressEachMultiCallCreatesDistinctView() {
            var first = empty.multi();
            var second = empty.multi();

            assertThat(first).isNotSameAs(second);
            assertThat(first.asText()).isEqualTo(second.asText());
        }
    }

    @Nested
    class Concurrency {

        @Test
        void stressConcurrentAccess() throws InterruptedException {
            var threadCount = 16;
            var iterationsPerThread = STRESS_ITERATIONS / threadCount;
            var executor = Executors.newFixedThreadPool(threadCount);
            var start = new CountDownLatch(1);
            var done = new CountDownLatch(threadCount);
            var scalarFailures = new AtomicInteger();
            var predicateFailures = new AtomicInteger();
            var multiFailures = new AtomicInteger();

            for (var t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (var i = 0; i < iterationsPerThread; i++) {
                            if (!empty.isEmpty() || empty.isNull() || empty.isInt() || empty.isDouble()) {
                                predicateFailures.incrementAndGet();
                            }

                            try {
                                empty.asText();
                                scalarFailures.incrementAndGet();
                            } catch (IllegalStateException ignored) {
                                // expected
                            }

                            var multi = empty.multi();
                            if (!multi.asText().isEmpty()
                                    || !multi.asInt().isEmpty()
                                    || !multi.asDouble().isEmpty()
                                    || !multi.asBoolean().isEmpty()) {
                                multiFailures.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            executor.shutdownNow();

            assertThat(scalarFailures).hasValue(0);
            assertThat(predicateFailures).hasValue(0);
            assertThat(multiFailures).hasValue(0);
        }
    }
}
