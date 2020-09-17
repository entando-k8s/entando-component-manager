package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.ComponentType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntandoBundleComponentJob {
    private UUID id;
    private ComponentType type;
    private String componentId;
    private String checksum;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;

    public static EntandoBundleComponentJob fromEntity(EntandoBundleComponentJobEntity entity) {
        return EntandoBundleComponentJob.builder()
                .id(entity.getId())
                .checksum(entity.getChecksum())
                .componentId(entity.getComponentId())
                .status(entity.getStatus())
                .type(entity.getComponentType())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
