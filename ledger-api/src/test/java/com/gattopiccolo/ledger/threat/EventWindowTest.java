package com.gattopiccolo.ledger.threat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventWindowTest {

    private static SecurityEvent event(int second, String actor) {
        return new SecurityEvent(Instant.parse("2026-11-08T10:00:00Z").plusSeconds(second), actor,
                null, "GET", "/api/accounts/1", 200, 1L, 1L, EventType.REQUEST);
    }

    @Test
    void oldestEventIsDroppedWhenCapacityIsReached() {
        var window = new EventWindow(2);
        window.add(event(0, "a"));
        window.add(event(1, "a"));
        window.add(event(2, "a"));

        assertEquals(2, window.recent(10, null).size());
        assertEquals(3, window.total());
        // newest first, and the oldest event (second 0) is the one that was evicted
        assertEquals(Instant.parse("2026-11-08T10:00:02Z"), window.recent(10, null).get(0).at());
        assertEquals(Instant.parse("2026-11-08T10:00:01Z"), window.recent(10, null).get(1).at());
    }

    @Test
    void recentCanBeFilteredByActorAndLimited() {
        var window = new EventWindow(10);
        window.add(event(0, "a"));
        window.add(event(1, "b"));
        window.add(event(2, "a"));

        assertEquals(2, window.recent(10, "a").size());
        assertEquals(1, window.recent(1, null).size());
    }

    @Test
    void byActorSinceExcludesEarlierEvents() {
        var window = new EventWindow(10);
        window.add(event(0, "a"));
        window.add(event(100, "a"));

        var from = Instant.parse("2026-11-08T10:00:00Z").plusSeconds(50);
        assertEquals(1, window.byActorSince("a", from).size());
    }
}
