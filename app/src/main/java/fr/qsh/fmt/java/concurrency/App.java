package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Application;
import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import net.jcip.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        @Override
        public Results execute(Parameters parameters) {
            // Pour chaque événement en entrée…
            parameters.input().onEvent(e -> {
                // on vérifie s'il est valide…
                parameters.verifier().verify(e);
                // on compte les événements vus…
                receivedCount++;
            });
            return new Results(receivedCount);
        }
    }

    public static class Parallel implements Application {
        @GuardedBy("this")
        private long receivedCount = 0;

        @Override
        public Results execute(Parameters parameters) {
            try (ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
                parameters.input().onEvent(e -> service.execute(() -> {
                    parameters.verifier().verify(e);
                    synchronized (this) {
                        receivedCount++;
                    }
                }));
            }
            //noinspection FieldAccessNotGuarded After the ExecutorService is closed, there is no need for guarding anymore
            return new Results(receivedCount);
        }
    }

    public static void main(String[] args) {
        final var simulator = new Simulator(ITERATION_COUNT);
        simulator.simulate(new App.Parallel());
    }
}
