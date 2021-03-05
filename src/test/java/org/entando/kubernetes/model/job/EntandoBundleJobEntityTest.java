package org.entando.kubernetes.model.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntandoBundleJobEntityTest {

    private final UUID id = UUID.randomUUID();
    private final String componentId = "compId";
    private final String componentName = "name";
    private final String componentVersion = "vers";
    private final LocalDateTime startedAt = LocalDateTime.now();
    private final LocalDateTime finishedAt = LocalDateTime.now().plusDays(1);
    private final String userId = "user";
    private final double progress = 0.5d;
    private final JobStatus status = JobStatus.INSTALL_COMPLETED;
    private final Integer installErrorCode = 6;
    private final String installErrorMessage = "error1";
    private final Integer rollbackErrorCode = 7;
    private final String rollbackErrorMessage = "error2";

    @Test
    void shouldCloneTheEntireEntity() {

        EntandoBundleJobEntity start = EntandoBundleJobEntity.builder()
                .id(id).componentId(componentId).componentName(componentName).componentVersion(componentVersion)
                .startedAt(startedAt).finishedAt(finishedAt).userId(userId).progress(progress).status(status)
                .installErrorCode(installErrorCode).installErrorMessage(installErrorMessage)
                .rollbackErrorCode(rollbackErrorCode).rollbackErrorMessage(rollbackErrorMessage).build();

        EntandoBundleJobEntity clone = start.clone();

        assertThat(clone).isEqualToComparingFieldByFieldRecursively(start);
    }
}
