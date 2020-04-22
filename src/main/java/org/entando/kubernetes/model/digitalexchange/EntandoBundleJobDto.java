package org.entando.kubernetes.model.digitalexchange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class EntandoBundleJobDto {

    private final UUID id;
    private final String componentId;
    private final String componentName;
    private final String componentVersion;
    private final JobStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final List<EntandoBundleComponentJob> componentJobs;

    public static EntandoBundleJobDto from(EntandoBundleJob job, List<EntandoBundleComponentJob> componentJobs) {
        return EntandoBundleJobDto.builder()
                .id(job.getId())
                .componentId(job.getComponentId())
                .componentName(job.getComponentName())
                .componentVersion(job.getComponentVersion())
                .componentJobs(componentJobs)
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .status(job.getStatus())
                .build();
    }
}
