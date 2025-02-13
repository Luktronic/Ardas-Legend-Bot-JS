package com.ardaslegends.service.time;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TimeFreezeServiceTest {

    private TimeFreezeService timeFreezeService;

    @BeforeEach
    public void setup() {
        timeFreezeService = Mockito.spy(new TimeFreezeService(Executors.newVirtualThreadPerTaskExecutor()));
    }

    @Test
    public void ensureStarting24hTimerWorks() {
        log.debug("Testing if starting a 24h timer works");
        log.debug("Current Thread: {}", Thread.currentThread());
        val list = new ArrayList<Integer>();


        val result = timeFreezeService.start24hTimer(() -> list.add(1));

        log.debug("Thread state after starting timer: {}", result.future().state().name());
        assertThat(result.future().isDone()).isFalse();
        assertThat(result.future().state()).isEqualTo(Future.State.RUNNING);
        result.future().cancel(true);

        log.info("Test passed: ensureStarting24hTimerWorks");
    }

    @Test
    public void ensure24hTimerFinishesProperly() {
        log.debug("Testing if 24h timer ends properly");
        log.debug("Current Thread: {}", Thread.currentThread());
        val list = new ArrayList<Integer>();

        Mockito.doAnswer(invocation -> {
            timeFreezeService.sleep(Duration.ofMillis(100));
            return null;
        })
        .when(timeFreezeService).sleep(Duration.ofHours(24));

        val result = timeFreezeService.start24hTimer(() -> list.add(1));

        log.debug("Thread state after starting timer: {}", result.future().state().name());
        assertThat(result.future().isDone()).isFalse();
        assertThat(result.future().state()).isEqualTo(Future.State.RUNNING);

        timeFreezeService.sleep(Duration.ofMillis(200));

        log.debug("Thread state after waiting 24h: {}", result.future().state().name());
        assertThat(result.future().isDone()).isTrue();
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo(1);
        log.info("Test passed: ensure24hTimerFinishesProperly");
    }
}
