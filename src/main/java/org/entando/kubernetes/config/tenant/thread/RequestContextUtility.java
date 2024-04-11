package org.entando.kubernetes.config.tenant.thread;

import java.util.concurrent.Callable;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
class RequestContextUtility {
    
    public static <T> Callable<T> wrapWithRequestContext(Callable<T> task) {
        CurrentRequestThreadState requestCurrentThreadState = CurrentRequestThreadState.currentRequestThreadState();

        log.info("wrapWithRequestContext - Callable - before entering lambda");

        return () -> {
            log.info("wrapWithRequestContext - Callable - after entering lambda");
            CurrentRequestThreadState.setCurrentThreadState(requestCurrentThreadState);
            try {
                log.info("wrapWithRequestContext - Callable - within try block");
                return task.call();
            } finally {
                log.info("wrapWithRequestContext - Callable - withing finally block");
                // once the task is complete, clear thread state
                CurrentRequestThreadState.clearCurrentThreadState();
            }
        };
    }

    public static Runnable wrapWithRequestContext(Runnable task) {
        CurrentRequestThreadState currentRequestThreadState = CurrentRequestThreadState.currentRequestThreadState();

        log.info("wrapWithRequestContext - Runnable - before entering lambda");

        return () -> {
            log.info("wrapWithRequestContext - Runnable - after entering lambda");
            CurrentRequestThreadState.setCurrentThreadState(currentRequestThreadState);
            try {
                log.info("wrapWithRequestContext - Runnable - within try block");
                task.run();
            } finally {
                log.info("wrapWithRequestContext - Runnable - withing finally block");
                // once the task is complete, clear thread state
                CurrentRequestThreadState.clearCurrentThreadState();
            }
        };
    }
}
