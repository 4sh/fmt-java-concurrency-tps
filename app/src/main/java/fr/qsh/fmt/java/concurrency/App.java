package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Application;
import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import fr.qsh.fmt.java.concurrency.simulation.Verifier;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

public class App {

    /**
     * Le nombre de messages qui seront envoyés pendant une simulation.
     *
     * <p>Pour une bonne expérience dans ce TP, une simulation doit prendre ~30 secondes.
     * Si vous lancez ce TP et que la simulation prend plusieurs minutes, nous vous recommandons
     * de baisser ce nombre.
     */
    public static final long ITERATION_COUNT = 10_000L;

    public static class Sequential implements Application {
        private long receivedCount = 0;
        private final HashMap<Integer, Long> eventsPerDoor = new HashMap<>();

        @Override
        public Results execute(Parameters parameters) {
            parameters.input().onEvent(e -> {
                double result = parameters.verifier().verify(e);
                if (result > 5) {
                    eventsPerDoor.merge(e.doorId(), 1L, Long::sum);
                }
                receivedCount++;
            });
            return new Results(receivedCount, eventsPerDoor);
        }
    }

    public static class Parallel implements Application {
        private final LongAdder receivedCount = new LongAdder();

        @Override
        public Results execute(Parameters parameters) {
            try (ExecutorService service = Executors.newFixedThreadPool(Verifier.MAX_CONCURRENCY)) {
                parameters.input().onEvent(e -> service.execute(() -> {
                    parameters.verifier().verify(e);
                    receivedCount.increment();
                }));
            }
            return new Results(receivedCount.intValue(), new HashMap<>());
        }
    }

    public static void main(String[] args) {
        final var simulator = new Simulator(ITERATION_COUNT);
        simulator.simulate(new App.Parallel());
    }
}
