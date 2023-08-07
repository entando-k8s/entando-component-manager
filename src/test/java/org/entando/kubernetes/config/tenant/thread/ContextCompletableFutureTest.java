package org.entando.kubernetes.config.tenant.thread;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ContextCompletableFutureTest {

    @Test
    void testRunAsyncWithContext() throws Exception {

        String exptected = "primary";

        TenantContextHolder.setCurrentTenantCode(exptected);

        CompletableFuture<Void> future = ContextCompletableFuture.runAsyncWithContext(() -> {
            // Simulate some asynchronous work
            try {
                Thread.sleep(100); // Sleep for 100ms
                assertThat(TenantContextHolder.getCurrentTenantCode()).isEqualTo(exptected);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        TenantContextHolder.setCurrentTenantCode("tenant 1");

        // Wait for the asynchronous operation to complete
        future.get();
    }
}