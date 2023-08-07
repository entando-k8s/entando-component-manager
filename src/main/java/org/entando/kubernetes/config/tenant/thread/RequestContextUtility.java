package org.entando.kubernetes.config.tenant.thread;

import java.util.concurrent.Callable;
import lombok.experimental.UtilityClass;

@UtilityClass
class RequestContextUtility {
    
    public static <T> Callable<T> wrapWithRequestContext(Callable<T> task) {
        CurrentRequestThreadState requestCurrentThreadState = CurrentRequestThreadState.currentRequestThreadState();
        return () -> {
            CurrentRequestThreadState.setCurrentThreadState(requestCurrentThreadState);
            try {
                return task.call();
            } finally {
                // once the task is complete, clear thread state
                CurrentRequestThreadState.clearCurrentThreadState();
            }
        };
    }

    public static Runnable wrapWithRequestContext(Runnable task) {
        CurrentRequestThreadState currentRequestThreadState = CurrentRequestThreadState.currentRequestThreadState();
        return () -> {
            CurrentRequestThreadState.setCurrentThreadState(currentRequestThreadState);
            try {
                task.run();
            } finally {
                // once the task is complete, clear thread state
                CurrentRequestThreadState.clearCurrentThreadState();
            }
        };
    }
}