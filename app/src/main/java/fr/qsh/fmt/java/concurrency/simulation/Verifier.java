package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;

public class Verifier {

    public Verifier() {}

    public Event verify(Event event) {
        double score = event.id();
        // Non-trivial math to simulate a database check
        for (int i = 0; i < 100_000; i++) {
            score = Math.sqrt(Math.abs(Math.sin(score) * Math.cos(i) * 100));
        }
        return new Event((long) score);
    }

}
