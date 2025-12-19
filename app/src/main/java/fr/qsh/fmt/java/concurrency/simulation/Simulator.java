package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Parameters;
import fr.qsh.fmt.java.concurrency.Results;

public class Simulator {

    private final long totalInput;

    public Simulator(long totalInput) {
        this.totalInput = totalInput;
    }

    public Results simulate(Application application) {
        final var parameters = new Parameters(new Input(totalInput));
        return application.execute(parameters);
    }
}
