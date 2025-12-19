package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApp {

    @Test
    void countIsCorrect() {
        final var count = 1_000_000_000L;
        final var simulator = new Simulator(count);
        final var results = simulator.simulate(new App.Sequential());
        assertEquals(count, results.receivedCount(), "After the simulation, the number of measured events is different than the number of events that were actually sent");
    }

}
