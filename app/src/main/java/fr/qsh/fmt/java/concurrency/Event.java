package fr.qsh.fmt.java.concurrency;

public sealed interface Event {

    int doorId();
    long eventId();

    record Ping(int doorId, long eventId) implements Event {}
}
