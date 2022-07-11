package org.entando.kubernetes.model.bundle.downloader;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MethodRetryerTest {


    @Test
    void executeShouldBeOk() {
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod(d -> {
            return EXECUTION_OK;
        }).checkerMethod(d -> {
            return EXECUTION_OK.equals(d);
        }).retries(1).build();
        assertThat(method.execute("test")).isEqualTo(EXECUTION_OK);
    }


    @Test
    void executeShouldRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        final String EXECUTION_OK = "OK";
        var method = MethodRetryer.<String, String>builder().execMethod(d -> {
            counter.incrementAndGet();
            return EXECUTION_OK;
        }).checkerMethod(d -> {
            return false;
        }).retries(3).build();
        method.execute("test");
        assertThat(counter.get()).isEqualTo(3);
    }

}
