package org.entando.kubernetes.service.digitalexchange.job;

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

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    TimerTask repeatedTask = new TimerTask() {
        public void run() {
            log.info("Post init task executing");
            // task ok
            try {

                service.install();

                log.info("Post init task executing");

                if (service.isCompleted() && !service.shouldRetry()) {
                    if (!executor.isShutdown()) {
                        executor.shutdown();
                        log.info("Post init task removed");
                    } else {
                        log.warn("PostInitProcessListener executor already shutdown");
                    }
                }

            } catch (Exception ex) {
                log.error("Error to execute post init task", ex);
            }
            log.info("Post init task executed");
        }

    };

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application ECR read start post-init");

        executor.scheduleWithFixedDelay(repeatedTask, START_DELAY, service.getFrequencyInSeconds(), TimeUnit.SECONDS);

    }

}
