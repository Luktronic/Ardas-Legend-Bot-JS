package com.ardaslegends.service.time;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;

public record Timer<T>(Future<T> future, OffsetDateTime finishesAt) {

    public Duration timeLeft() {
        return Duration.between(OffsetDateTime.now(), finishesAt);
    }
}
