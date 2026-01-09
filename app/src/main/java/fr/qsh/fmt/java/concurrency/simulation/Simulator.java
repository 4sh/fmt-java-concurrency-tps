package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Parameters;
import fr.qsh.fmt.java.concurrency.Results;

import java.util.Random;

public class Simulator {

    private final long totalInput;
    private final Random random = new Random(42);

    public Simulator(long totalInput) {
        this.totalInput = totalInput;
    }

    public Results simulate(Application application) {
        final var parameters = new Parameters(new Input(totalInput, random), new Verifier());
        return application.execute(parameters);
    }
}
