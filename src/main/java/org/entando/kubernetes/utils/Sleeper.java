package org.entando.kubernetes.utils;

public interface Sleeper {

    void sleep(long millis) throws InterruptedException;
}
