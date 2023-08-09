/**
 * execute the async runnable using the executor able to copy thread context to the child thread.
 */

package org.entando.kubernetes.config.tenant.thread;

import java.util.concurrent.CompletableFuture;

public class ContextCompletableFuture<T> extends CompletableFuture<T> {

    public static CompletableFuture<Void> runAsyncWithContext(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, new RequestContextSavingForkJoinPool());
    }
}
