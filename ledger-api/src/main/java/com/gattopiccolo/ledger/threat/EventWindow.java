package com.gattopiccolo.ledger.threat;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded, in-memory buffer of the most recent security events. Rules query it
 * for sliding-window aggregates (counts, distinct accounts) per actor.
 *
 * <p>This is a prototype stand-in for a real event store. It is intentionally
 * small: when the capacity is reached the oldest event is dropped.
 */
public final class EventWindow {

    private final int capacity;
    private final Deque<SecurityEvent> events = new ArrayDeque<>();
    private final AtomicLong total = new AtomicLong();

    public EventWindow(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized void add(SecurityEvent event) {
        if (events.size() == capacity) {
            events.removeFirst();
        }
        events.addLast(event);
        total.incrementAndGet();
    }

    /** Number of events ever added (not just those still buffered). */
    public long total() {
        return total.get();
    }

    /** Events of one actor at or after {@code from}, oldest first. */
    public synchronized List<SecurityEvent> byActorSince(String actor, Instant from) {
        List<SecurityEvent> result = new ArrayList<>();
        for (SecurityEvent e : events) {
            if (Objects.equals(e.actor(), actor) && !e.at().isBefore(from)) {
                result.add(e);
            }
        }
        return result;
    }

    /** Most recent events, newest first, optionally restricted to one actor. */
    public synchronized List<SecurityEvent> recent(int limit, String actor) {
        List<SecurityEvent> result = new ArrayList<>();
        Iterator<SecurityEvent> newestFirst = events.descendingIterator();
        while (newestFirst.hasNext() && result.size() < limit) {
            SecurityEvent e = newestFirst.next();
            if (actor == null || actor.equals(e.actor())) {
                result.add(e);
            }
        }
        return result;
    }
}
