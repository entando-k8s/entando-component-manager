package org.entando.kubernetes.model.bundle.downloader;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MethodRetryerTest {


    @Test
    void executeShouldBeOk() throws Exception {
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            return EXECUTION_OK;
        }).checkerMethod((d, ex, execNumber) -> {
            return ex == null && EXECUTION_OK.equals(d);
        }).retries(1).waitFor(1).build();
        assertThat(method.execute("test")).isEqualTo(EXECUTION_OK);
    }


    @Test
    void executeShouldRetry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            return EXECUTION_OK;
        }).checkerMethod((d, ex, execNumber) -> {
            return ex != null || false;
        }).retries(3).waitFor(1).build();
        method.execute("test");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void executeShouldRetryWithExceptionToRetry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            throw new Exception();
        }).checkerMethod((d, ex, execNumber) -> {
            return ex == null || false;
        }).retries(3).waitFor(1).build();
        Assert.assertThrows(Exception.class, () -> method.execute("test"));
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void executeShouldRetryWithExceptionWithoutRetry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod((d, execNumber) -> {
            counter.incrementAndGet();
            throw new IllegalArgumentException();
        }).checkerMethod((d, ex, execNumber) -> {
            return true;
        }).retries(3).waitFor(1).build();
        Assert.assertThrows(IllegalArgumentException.class, () -> method.execute("test"));
        assertThat(counter.get()).isEqualTo(1);
    }

}
