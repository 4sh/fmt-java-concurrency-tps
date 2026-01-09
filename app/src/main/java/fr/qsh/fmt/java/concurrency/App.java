package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Application;
import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import fr.qsh.fmt.java.concurrency.simulation.Verifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

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
        private final List<Consumer<Event>> observers = new ArrayList<>();

        @Override
        public Results execute(Parameters parameters) {
            parameters.input().onEvent(e -> {
                switch (e) {
                    case Event.Ping ping -> {
                        double result = parameters.verifier().verify(e);
                        if (result > 5) {
                            eventsPerDoor.merge(ping.doorId(), 1L, Long::sum);
                        }
                        if (result > 7) {
                            observers.forEach(observer -> observer.accept(e));
                        }
                    }
                    case Event.RegisterObserver register -> observers.add(register.consumer());
                }
                receivedCount++;
            });
            return new Results(receivedCount, eventsPerDoor);
        }
    }

    public static class Parallel implements Application {
        private final LongAdder receivedCount = new LongAdder();
        private final ConcurrentHashMap<Integer, LongAdder> eventsPerDoor = new ConcurrentHashMap<>();

        @Override
        public Results execute(Parameters parameters) {
            try (ExecutorService service = Executors.newFixedThreadPool(Verifier.MAX_CONCURRENCY)) {
                parameters.input().onEvent(e -> service.execute(() -> {
                    switch (e) {
                        case Event.Ping ping -> {
                            double result = parameters.verifier().verify(e);
                            if (result > 5) {
                                eventsPerDoor.computeIfAbsent(ping.doorId(), k -> new LongAdder())
                                        .increment();
                            }
                        }
                        case Event.RegisterObserver register -> {
                            // TODO
                        }
                    }
                    receivedCount.increment();
                }));
            }
            final var resultsMap = new HashMap<Integer, Long>(eventsPerDoor.size());
            eventsPerDoor.forEach((k, v) -> resultsMap.put(k, v.sum()));
            return new Results(receivedCount.sum(), resultsMap);
        }
    }

    public static void main(String[] args) {
        final var simulator = new Simulator(ITERATION_COUNT, e -> System.err.println("Important event: " + e));
        simulator.simulate(new App.Parallel());
    }
}
