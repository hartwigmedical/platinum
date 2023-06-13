package com.hartwig.platinum.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delay {

    private static final Logger LOGGER = LoggerFactory.getLogger(Delay.class);
    private final long delay;
    private final Unit unit;

    public enum Unit {
        MINUTES(1000 * 60),
        SECONDS(1000),
        MILLISECONDS(1);

        final long millisecondsPer;
        Unit(long millisecondsPer) {
            this.millisecondsPer = millisecondsPer;
        }
    }

    private Delay(long delay, Unit unit) {
        this.delay = delay;
        this.unit = unit;
    }

    static Delay forMinutes(final int minutes) {
        return new Delay(minutes, Unit.MINUTES);
    }

    static Delay forSeconds(final int seconds) {
        return new Delay(seconds, Unit.SECONDS);
    }

    static Delay forMilliseconds(final int milliseconds) {
        return new Delay(milliseconds, Unit.MILLISECONDS);
    }

    public void threadSleep() {
        try {
            Thread.sleep(toMillis());
        } catch (InterruptedException e) {
            LOGGER.error("Thread interupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public long toMillis() {
        return delay * unit.millisecondsPer;
    }

    @Override
    public String toString() {
        return String.format("%d %s", delay, unit.name().toLowerCase());
    }
}
