package dev.vepo.jsonata.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import dev.vepo.jsonata.JSONata;
import dev.vepo.jsonata.functions.json.ImmutableJacksonDataInspector;
import dev.vepo.jsonata.functions.json.MutableJacksonDataInspector;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(0)
public class JsonataPerformanceBenchmark {

    @State(Scope.Benchmark)
    public static class PerformanceCase000 {

        private JSONata jsonata;
        private String input;

        @Setup(Level.Trial)
        public void setUp() throws Exception {
            var fixture = PerformanceCaseFixture.load("case000");
            jsonata = JSONata.jsonata(fixture.expr());
            input = fixture.inputJson();
        }

        @Benchmark
        public void evaluate() {
            jsonata.evaluate(input);
        }
    }

    @State(Scope.Benchmark)
    public static class PerformanceCase001 {

        private JSONata jsonata;
        private String input;

        @Setup(Level.Trial)
        public void setUp() throws Exception {
            var fixture = PerformanceCaseFixture.load("case001");
            jsonata = JSONata.jsonata(fixture.expr());
            input = fixture.inputJson();
        }

        @Benchmark
        public void evaluate() {
            jsonata.evaluate(input);
        }
    }

    @State(Scope.Benchmark)
    public static class RegexHeavy {

        private JSONata jsonata;
        private String input;

        @Setup(Level.Trial)
        public void setUp() {
            jsonata = JSONata.jsonata("$count($split(content, /\\s+/))");
            input = """
                    {"content": "%s"}
                    """.formatted("word ".repeat(10_000).trim());
        }

        @Benchmark
        public void evaluateSplit() {
            jsonata.evaluate(input);
        }
    }

    @State(Scope.Benchmark)
    public static class InspectorComparison {

        private JSONata mutableJsonata;
        private JSONata immutableJsonata;
        private String input;

        @Setup(Level.Trial)
        public void setUp() throws Exception {
            var fixture = PerformanceCaseFixture.load("case000");
            mutableJsonata = JSONata.jsonata(fixture.expr(), MutableJacksonDataInspector.INSTANCE);
            immutableJsonata = JSONata.jsonata(fixture.expr(), ImmutableJacksonDataInspector.INSTANCE);
            input = fixture.inputJson();
        }

        @Benchmark
        public void mutableInspector() {
            mutableJsonata.evaluate(input);
        }

        @Benchmark
        public void immutableInspector() {
            immutableJsonata.evaluate(input);
        }
    }

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(".*JsonataPerformanceBenchmark.*")
                .build())
                .run();
    }
}
