package org.entando.kubernetes.model.job;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final String errorMesage;
    private final List<BundleComponentJobDto> componentJobs;

    public static EntandoBundleJobDto from(EntandoBundleJob job, List<EntandoBundleComponentJob> componentJobs) {
        return EntandoBundleJobDto.builder()
                .id(job.getId())
                .componentId(job.getComponentId())
                .componentName(job.getComponentName())
                .componentVersion(job.getComponentVersion())
                .componentJobs(
                        componentJobs.stream().map(cj -> BundleComponentJobDto.builder()
                                .id(cj.getId())
                                .checksum(cj.getChecksum())
                                .componentId(cj.getComponentId())
                                .status(cj.getStatus())
                                .type(cj.getComponentType())
                                .startedAt(cj.getStartedAt())
                                .finishedAt(cj.getFinishedAt())
                                .errorMessage(cj.getErrorMessage())
                                .build()).collect(Collectors.toList())
                )
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .status(job.getStatus())
                .errorMesage(job.getErrorMessage())
                .build();
    }

    @Data
    @Builder
    @RequiredArgsConstructor
    public static class BundleComponentJobDto {

        private final UUID id;
        private final ComponentType type;
        private final String componentId;
        private final String checksum;
        private final JobStatus status;
        private final LocalDateTime startedAt;
        private final LocalDateTime finishedAt;
        private final String errorMessage;
    }
}
