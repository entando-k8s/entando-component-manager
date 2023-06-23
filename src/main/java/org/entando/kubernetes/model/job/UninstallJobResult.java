package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UninstallJobResult {

    private UUID id;
    private String componentId;
    private String componentName;
    private String componentVersion;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String userId;
    private double progress;
    private JobStatus status;
    private Integer rollbackErrorCode;
    private String rollbackErrorMessage;
    private Integer uninstallErrorCode;
    private String uninstallErrorMessage;
    private String uninstallErrors;


    public static Optional<UninstallJobResult> fromEntity(EntandoBundleJobEntity entity) {
        return Optional.ofNullable(entity)
                .map(c -> UninstallJobResult.builder()
                        .id(entity.getId())
                        .componentId(entity.getComponentId())
                        .componentName(entity.getComponentName())
                        .componentVersion(entity.getComponentVersion())
                        .startedAt(entity.getStartedAt())
                        .finishedAt(entity.getFinishedAt())
                        .userId(entity.getUserId())
                        .progress(entity.getProgress())
                        .status(entity.getStatus())
                        .rollbackErrorCode(entity.getRollbackErrorCode())
                        .rollbackErrorMessage(entity.getRollbackErrorMessage())
                        .uninstallErrorCode(entity.getUninstallErrorCode())
                        .uninstallErrorMessage(entity.getUninstallErrorMessage())
                        .uninstallErrors(entity.getUninstallErrors())
                        .build());
    }

}
