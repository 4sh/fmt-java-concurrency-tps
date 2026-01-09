package fr.qsh.fmt.java.concurrency.simulation;

import fr.qsh.fmt.java.concurrency.Event;

import java.util.Random;
import java.util.function.Consumer;

public class Input {

    private final long totalInput;
    private final Random random;
    private final Consumer<Event> observer;

    public Input(long totalInput, Random random, Consumer<Event> observer) {
        this.totalInput = totalInput;
        this.random = random;
        this.observer = observer;
    }

    public void onEvent(Consumer<Event> consumer) {
        for (long i = 0; i < totalInput; i++) {
            if (i == 0 || i == totalInput/3) {
                consumer.accept(new Event.RegisterObserver(observer, i));
            } else {
                consumer.accept(new Event.Ping(random.nextInt(100), i));
            }
        }
    }

}
