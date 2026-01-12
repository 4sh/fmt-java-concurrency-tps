package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Simulator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestApp {

    private class Observer implements Consumer<Event> {
        LongAdder calledCount = new LongAdder();

        @Override
        public void accept(Event event) {
            calledCount.increment();
        }
    }

    @Test
    void countIsCorrect_Sequential() {
        final var count = App.ITERATION_COUNT;
        final var simulator = new Simulator(count, new Observer());
        final var results = simulator.simulate(new App.Sequential());
        assertEquals(count, results.receivedCount(), "After the simulation, the number of measured events is different than the number of events that were actually sent");
    }

    @Test
    void countIsCorrect_Parallel() {
        final var count = App.ITERATION_COUNT;
        final var seqObserver = new Observer();
        final var parObserver = new Observer();
        final var resultsExpected = new Simulator(count, seqObserver).simulate(new App.Sequential());
        final var resultsActual = new Simulator(count, parObserver).simulate(new App.Parallel());
        assertEquals(resultsExpected, resultsActual);
        assertEquals(5635, parObserver.calledCount.sum());
    }

}
