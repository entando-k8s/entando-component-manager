package org.entando.kubernetes.model.bundle.downloader;

import java.util.function.Predicate;
import lombok.Builder;

@Builder
public class MethodRetryer<I, O> {

    private RetryerExecutor<I, O> execMethod = null;
    private Predicate<O> checkerMethod = null;
    private int retries = 3;

    public O execute(I input) {
        O result = null;
        boolean success = false;
        int executionNumber = 0;
        while (!success || executionNumber < retries) {
            result = execMethod.exec(input);
            executionNumber++;
            success = checkerMethod.test(result);
            if (executionNumber >= retries) {
                break;
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface RetryerExecutor<I, O> {

        O exec(I input);
    }
}
