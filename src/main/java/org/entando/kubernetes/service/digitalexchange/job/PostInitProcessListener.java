package org.entando.kubernetes.service.digitalexchange.job;

import java.time.Duration;
import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
@RequiredArgsConstructor
public class PostInitProcessListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final long START_DELAY = 1;
    private final PostInitService service;
    private final PostInitConfigurationService configuration;
    private Instant startTime;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    TimerTask repeatedTask = new TimerTask() {
        public void run() {
            log.debug("Post init task executing");
            // task ok
            try {
                if (isMaxWaitExpired()) {
                    log.info("Post init timeout while waiting for the application to get ready");
                    removeTask();
                } else {
                    service.install();

                    log.debug("Post init task executed");
                    if (service.isCompleted() && !service.shouldRetry()) {
                        removeTask();
                    }
                }
            } catch (Exception ex) {
                log.error("Error to execute post init task", ex);
            }
        }

        private void removeTask() {
            if (!executor.isShutdown()) {
                executor.shutdown();
                log.info("End Post init process");
            } else {
                log.warn("PostInitProcessListener executor already shutdown");
            }
        }

        private boolean isMaxWaitExpired() {
            Instant finishTime = Instant.now();
            long timeElapsed = Duration.between(startTime, finishTime).toSeconds();
            boolean maxWaitExpired = timeElapsed >= configuration.getMaxAppWaitInSeconds();
            log.trace("isMaxWaitExpired ? '{}' elapsed time:'{}'s, maxAppWait:'{}'s", maxWaitExpired, timeElapsed,
                    configuration.getMaxAppWaitInSeconds());
            return maxWaitExpired;
        }

    };

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Waiting for the EntandoApp to get ready");
        startTime = Instant.now();
        executor.scheduleWithFixedDelay(repeatedTask, START_DELAY, configuration.getFrequencyInSeconds(),
                TimeUnit.SECONDS);

    }

}
