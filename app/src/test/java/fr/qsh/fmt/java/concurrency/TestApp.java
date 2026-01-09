package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApp {

    @Test
    void countIsCorrect_Sequential() {
        final var count = App.ITERATION_COUNT;
        final var simulator = new Simulator(count);
        final var results = simulator.simulate(new App.Sequential());
        assertEquals(count, results.receivedCount(), "After the simulation, the number of measured events is different than the number of events that were actually sent");
    }

    @Test
    void countIsCorrect_Parallel() {
        final var count = App.ITERATION_COUNT;
        final var resultsExpected = new Simulator(count).simulate(new App.Sequential());
        final var resultsActual = new Simulator(count).simulate(new App.Parallel());
        assertEquals(resultsExpected, resultsActual);
    }

}
