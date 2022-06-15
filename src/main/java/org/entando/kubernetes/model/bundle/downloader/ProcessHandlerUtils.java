package org.entando.kubernetes.model.bundle.downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public final class ProcessHandlerUtils {

    private ProcessHandlerUtils() {
    }

    /**
     * Utility to kill all processes whom command line start with a given string.
     *
     * @param name the string to use to search the processes to kill
     */
    public static void killProcessStartWithName(String name) {
        try {

            ProcessHandle.allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.startsWith(name)).orElse(false))
                    .map(p -> {
                        log.debug("Found process commandLine:'{}' it starts with:'{}' i'm going to kill it",
                                p.info().commandLine(), name);
                        return p;
                    }).findFirst()
                    .ifPresent(ProcessHandle::destroy);

        } catch (Exception ex) {
            log.warn("kill process start with name:'{}' failed found error", name, ex);
        }
    }

    /**
     * Utility to kill all processes that contain a given string in the command line.
     *
     * @param name the string to use to search the processes to kill
     */
    public static void killProcessContainsName(String name) {
        try {

            ProcessHandle.allProcesses()
                    .filter(p -> p.info().commandLine().map(c -> c.contains(name)).orElse(false))
                    .map(p -> {
                        log.debug("Found process commandLine:'{}' it contains:'{}'  i'm going to kill it",
                                p.info().commandLine(), name);
                        return p;
                    })
                    .findFirst()
                    .ifPresent(ProcessHandle::destroy);

        } catch (Exception ex) {
            log.warn("kill process contains name:'{}' failed found error", name, ex);
        }
    }

}
