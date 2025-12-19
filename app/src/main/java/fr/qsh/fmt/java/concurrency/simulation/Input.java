package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;

import java.util.function.Consumer;

public class Input {

    private final long totalInput;

    public Input(long totalInput) {
        this.totalInput = totalInput;
    }

    public void onEvent(Consumer<Event> consumer) {
        for (long i = 0; i < totalInput; i++) {
            consumer.accept(new Event(i));
        }
    }

}
