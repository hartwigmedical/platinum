package com.hartwig.platinum.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Delay {

    Logger LOGGER = LoggerFactory.getLogger(Delay.class);

    void forMinutes(final int minutes);

    static Delay threadSleep() {
        return minutes -> {
            try {
                Thread.sleep(minutes * 60 * 1000);
            } catch (InterruptedException e) {
                LOGGER.error("Thread interupted", e);
            }
        };
    }
}
