package org.entando.kubernetes.utils;

import static org.mockito.Mockito.doAnswer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SleepStubber {

    public static org.mockito.stubbing.Stubber doSleep(Duration timeUnit) {
        return doAnswer(invocationOnMock -> {
            TimeUnit.MILLISECONDS.sleep(timeUnit.toMillis());
            return null;
        });
    }

    public static <E> org.mockito.stubbing.Stubber doSleep(Duration timeUnit, E ret) {
        return doAnswer(invocationOnMock -> {
            TimeUnit.MILLISECONDS.sleep(timeUnit.toMillis());
            return ret;
        });
    }

}
