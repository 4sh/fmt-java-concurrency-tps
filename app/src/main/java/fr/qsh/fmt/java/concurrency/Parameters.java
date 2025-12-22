package fr.qsh.fmt.java.concurrency;

import fr.qsh.fmt.java.concurrency.simulation.Input;
import fr.qsh.fmt.java.concurrency.simulation.Verifier;

public record Parameters(Input input, Verifier verifier) {
}
