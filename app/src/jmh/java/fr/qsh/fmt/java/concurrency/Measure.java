package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Application;
import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BenchmarkMode({Mode.AverageTime})
@Fork(1)
@Measurement(iterations = 2, time = 3)
@Warmup(iterations = 1, time = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
public class Measure {

    private static final long EVENT_COUNT = App.ITERATION_COUNT;

    private void measure(Application app, Blackhole blackhole) {
        final var simulator = new Simulator(EVENT_COUNT, blackhole::consume);
        final var results = simulator.simulate(app);
        assertEquals(EVENT_COUNT, results.receivedCount(), "After the simulation, the number of measured events is different than the number of events that were actually sent");
        blackhole.consume(results.receivedCount());
    }

    @Benchmark
    public void sequential(Blackhole blackhole) {
        measure(new App.Sequential(), blackhole);
    }

    @Benchmark
    public void parallel(Blackhole blackhole) {
        measure(new App.Parallel(), blackhole);
    }
}
