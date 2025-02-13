package com.ardaslegends.service.time;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

public interface Sleep {

    default void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default void sleepUntil(Temporal date) {
        sleep(Duration.between(OffsetDateTime.now(), date));
    }
}
