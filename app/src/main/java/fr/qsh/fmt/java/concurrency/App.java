package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Application;
import fr.qsh.fmt.java.concurrency.simulation.Simulator;

public class App {

    public static class Sequential implements Application {
        long receivedCount = 0;

        @Override
        public Results execute(Parameters parameters) {
            parameters.input().onEvent(e -> receivedCount++);
            return new Results(receivedCount);
        }
    }

    public static void main(String[] args) {
        final var simulator = new Simulator(100_000_000_000L);
        simulator.simulate(new App.Sequential());
    }
}
