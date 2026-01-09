package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;

import java.util.Random;
import java.util.function.Consumer;

public class Input {

    private final long totalInput;
    private final Random random;

    public Input(long totalInput, Random random) {
        this.totalInput = totalInput;
        this.random = random;
    }

    public void onEvent(Consumer<Event> consumer) {
        for (long i = 0; i < totalInput; i++) {
            consumer.accept(new Event.Ping(random.nextInt(100), i));
        }
    }

}
