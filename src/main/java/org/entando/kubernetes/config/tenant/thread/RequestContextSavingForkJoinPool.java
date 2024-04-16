package org.entando.kubernetes.config.tenant.thread;

import static org.entando.kubernetes.config.tenant.thread.RequestContextUtility.wrapWithRequestContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class RequestContextSavingForkJoinPool extends ForkJoinPool {

    private static RequestContextSavingForkJoinPool instance;

    private RequestContextSavingForkJoinPool() {
        super();
    }

    public static synchronized RequestContextSavingForkJoinPool getInstance() {
        if (instance == null) {
            instance = new RequestContextSavingForkJoinPool();
        }
        return instance;
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return super.submit(wrapWithRequestContext(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return super.submit(wrapWithRequestContext(task), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        return super.submit(wrapWithRequestContext(task));
    }

    @Override
    public void execute(Runnable task) {
        super.execute(wrapWithRequestContext(task));
    }
}
