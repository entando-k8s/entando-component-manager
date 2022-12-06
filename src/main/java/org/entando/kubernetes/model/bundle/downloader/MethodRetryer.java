package org.entando.kubernetes.model.bundle.downloader;

import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class MethodRetryer<I, O> {

    private RetryerExecutor<I, O> execMethod = null;
    private RetryerChecker<O> checkerMethod = null;
    @Builder.Default
    private RetryerWaitStrategy waitStrategy = new DefaultRetryerWaitStrategy();
    @Builder.Default
    private int retries = 3;
    @Builder.Default
    private TimeUnit unit = TimeUnit.SECONDS;
    @Builder.Default
    private long waitFor = 5;


    public O execute(I input) {
        O result = null;
        Exception ex = null;
        boolean success = false;
        int executionNumber = 0;
        while (!success) {
            ex = null;
            try {
                result = execMethod.exec(input);
            } catch (Exception e) {
                ex = e;
            }
            executionNumber++;
            success = checkerMethod.test(result, ex);
            if (executionNumber >= retries) {

                manageErrorToThrowRuntime(ex);
                break;
            }
            waitStrategy.wait(unit, waitFor, executionNumber);

        }
        manageErrorToThrowRuntime(ex);
        return result;
    }

    private void manageErrorToThrowRuntime(Exception ex) {
        if (ex != null) {
            if (!(ex instanceof RuntimeException)) {
                ex = new RuntimeException(ex.getMessage(), ex);
            }
            throw (RuntimeException) ex;
        }
    }

    @FunctionalInterface
    public interface RetryerExecutor<I, O> {

        O exec(I input) throws Exception;
    }

    @FunctionalInterface
    public interface RetryerChecker<O> {

        boolean test(O input, Exception ex);
    }

    public interface RetryerWaitStrategy {

        void wait(TimeUnit time, long waitFor, int executionNumber);
    }

    public static class DefaultRetryerWaitStrategy implements RetryerWaitStrategy {

        public void wait(TimeUnit time, long waitFor, int executionNumber) {
            try {
                Thread.sleep(time.toMillis(waitFor));
            } catch (InterruptedException ex) {
                log.error("error wait for retryer", ex);
                Thread.currentThread().interrupt();
            }
        }

    }
}
