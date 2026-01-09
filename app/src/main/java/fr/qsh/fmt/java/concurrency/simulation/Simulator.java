package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;
import fr.qsh.fmt.java.concurrency.Parameters;
import fr.qsh.fmt.java.concurrency.Results;

import java.util.Random;
import java.util.function.Consumer;

public class Simulator {

    private final long totalInput;
    private final Random random = new Random(42);
    private final Consumer<Event> observer;

    public Simulator(long totalInput, Consumer<Event> observer) {
        this.totalInput = totalInput;
        this.observer = observer;
    }

    public Results simulate(Application application) {
        final var parameters = new Parameters(new Input(totalInput, random, observer), new Verifier());
        return application.execute(parameters);
    }
}
