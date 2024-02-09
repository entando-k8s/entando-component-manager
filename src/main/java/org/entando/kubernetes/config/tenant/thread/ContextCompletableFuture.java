/**
 * execute the async runnable using the executor able to copy thread context to the child thread.
 */

package org.entando.kubernetes.config.tenant.thread;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ContextCompletableFuture<T> extends CompletableFuture<T> {

    public static CompletableFuture<Void> runAsyncWithContext(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, new RequestContextSavingForkJoinPool());
    }

    public static <U> CompletableFuture<U> supplyAsyncWithContext(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, new RequestContextSavingForkJoinPool());
    }
}
