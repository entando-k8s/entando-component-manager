package org.entando.kubernetes.model.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;

@Slf4j
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
    private List<EntandoCoreComponentDelete> errorComponents;


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
                        .errorComponents(deserialize(entity.getUninstallErrors()))
                        .build());
    }

    public static List<EntandoCoreComponentDelete> deserialize(String value) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            return objectMapper.readValue(value, new TypeReference<List<EntandoCoreComponentDelete>>() {
            });
        } catch (Exception ex) {
            log.error("Error deserialize:'{}'", value, ex);
            return null;
        }
    }

}
