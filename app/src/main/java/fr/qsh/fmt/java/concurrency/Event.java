package fr.qsh.fmt.java.concurrency;

import java.util.function.Consumer;

public sealed interface Event {

    long eventId();

    record Ping(int doorId, long eventId) implements Event {}

    record RegisterObserver(Consumer<Event> consumer, long eventId) implements Event {}
}
