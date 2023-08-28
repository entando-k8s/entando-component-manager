package org.entando.kubernetes.service.digitalexchange.job;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@Tag("unit")
@ExtendWith(TenantContextJunitExt.class)
class PostInitProcessListenerTest {

    private static Properties propsBackup;
    private static int POST_INIT_CONFIG_TIMEOUT = 5;
    private static int POST_INIT_CONFIG_FREQUENCY = 1;

    private PostInitProcessListener postInitProcessListener;
    private PostInitService postInitService;

    @BeforeEach
    public void setup() throws Exception {
        postInitService = Mockito.mock(PostInitServiceImpl.class);

    }

    @AfterEach
    public void teardown() {
    }


    @Test
    void testTimerTask_ShouldWaitTimeout() {
        when(postInitService.getFrequencyInSeconds()).thenReturn(POST_INIT_CONFIG_FREQUENCY);
        when(postInitService.getMaxAppWaitInSeconds()).thenReturn(POST_INIT_CONFIG_TIMEOUT);
        when(postInitService.isCompleted()).thenReturn(true);
        when(postInitService.shouldRetry()).thenReturn(true);

        ApplicationReadyEvent event = Mockito.mock(ApplicationReadyEvent.class);
        postInitProcessListener = new PostInitProcessListener(postInitService);
        postInitProcessListener.onApplicationEvent(event);

        try {
            await().atMost(Duration.ofSeconds(POST_INIT_CONFIG_TIMEOUT * 2)).until(() -> false);
        } catch (Exception ex) {
            log.debug("test end");
        }
    }

}