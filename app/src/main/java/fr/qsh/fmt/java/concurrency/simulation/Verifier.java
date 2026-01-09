package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Verifier {

    private final Set<Long> currentThreads = ConcurrentHashMap.newKeySet();
    public static final int MAX_CONCURRENCY = 5;

    public Verifier() {}

    public double verify(Event event) {
        final var currentThread = Thread.currentThread();

        synchronized (this) {
            if (currentThreads.contains(currentThread.threadId())) {
                throw new IllegalStateException("Impossible situation: the current thread (" + currentThread + ") is known to be one of the concurrent callers but it is also trying to initiate a new call");
            } else if (currentThreads.size() >= MAX_CONCURRENCY) {
                throw new IllegalStateException("The verification service only supports a maximum of " + MAX_CONCURRENCY + " concurrent connections.\nKnown callers: " + currentThreads + "\nAttempted to call with the new thread: " + currentThread.threadId());
            } else {
                currentThreads.add(currentThread.threadId());
            }
        }

        double score = event.eventId();
        // Non-trivial math to simulate a database check
        for (int i = 0; i < 100_000; i++) {
            score = Math.sqrt(Math.abs(Math.sin(score) * Math.cos(i) * 100));
        }

        currentThreads.remove(currentThread.threadId());

        return score;
    }

}
