package org.entando.kubernetes.model.bundle.downloader;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;
import org.entando.kubernetes.model.bundle.downloader.MethodRetryer.DefaultRetryerWaitStrategy;
import org.entando.kubernetes.utils.ThreadSleeper;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MethodRetryerTest {


    @Test
    void executeShouldBeOk() {
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            return EXECUTION_OK;
        }).checkerMethod((d, ex, execNumber) -> {
            return ex == null && EXECUTION_OK.equals(d);
        }).retries(1).waitFor(1).build();
        assertThat(method.execute("test")).isEqualTo(EXECUTION_OK);
    }


    @Test
    void executeShouldRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            return EXECUTION_OK;
        }).checkerMethod((d, ex, execNumber) -> ex != null).retries(3).waitFor(1).build();
        method.execute("test");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void executeShouldRetryWithExceptionToRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            throw new Exception();
        }).checkerMethod((d, ex, execNumber) -> ex == null).retries(3).waitFor(1).build();
        Assert.assertThrows(Exception.class, () -> method.execute("test"));
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void executeShouldRetryWithExceptionWithoutRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            throw new IllegalArgumentException();
        }).checkerMethod((d, ex, execNumber) -> true).retries(3).waitFor(1).build();
        Assert.assertThrows(IllegalArgumentException.class, () -> method.execute("test"));
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldNotWaitIfSuccess() throws InterruptedException {
        final var mockSleeper = spy(ThreadSleeper.class);
        final var strategy = DefaultRetryerWaitStrategy.builder().sleeper(mockSleeper).build();
        final var method = MethodRetryer.<String, String>builder().waitStrategy(strategy)
                .execMethod((d, execNumber) -> "OK").checkerMethod((d, ex, execNumber) -> true).retries(2).waitFor(1)
                .build();
        method.execute("test");
        verify(mockSleeper, times(0)).sleep(anyInt());
    }

    @Test
    void shouldWaitIfError() throws InterruptedException {
        final var mockSleeper = spy(ThreadSleeper.class);
        final var strategy = DefaultRetryerWaitStrategy.builder().sleeper(mockSleeper).build();
        final var method = MethodRetryer.<String, String>builder().waitStrategy(strategy)
                .execMethod((d, execNumber) -> "KO").checkerMethod((d, ex, execNumber) -> false).retries(2).waitFor(1)
                .build();
        method.execute("test");
        verify(mockSleeper, times(1)).sleep(1000L);
    }
}
