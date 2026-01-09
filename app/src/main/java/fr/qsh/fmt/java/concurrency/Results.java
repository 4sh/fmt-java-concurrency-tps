package fr.qsh.fmt.java.concurrency;

import java.util.Map;

public record Results(
        long receivedCount,
        Map<Integer, Long> eventsPerDoor
) {
}
