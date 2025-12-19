package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Parameters;
import fr.qsh.fmt.java.concurrency.Results;

@FunctionalInterface
public interface Application {

    Results execute(Parameters parameters);
}
