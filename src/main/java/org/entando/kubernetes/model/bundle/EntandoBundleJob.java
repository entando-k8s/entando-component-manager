/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.entando.kubernetes.model.bundle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.TrackableJob;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntandoBundleJob implements TrackableJob {
    private UUID id;
    private String componentId;
    private String componentName;
    private String componentVersion;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private JobStatus status;
    private String errorMessage;

    private List<EntandoBundleComponentJob> componentJobs;

    public static EntandoBundleJob fromEntity(EntandoBundleJobEntity entity) {
        return fromEntity(entity, new ArrayList<>());
    }

    public static EntandoBundleJob fromEntity(EntandoBundleJobEntity entity, List<EntandoBundleComponentJobEntity> componentJobs) {
        return EntandoBundleJob.builder()
                .id(entity.getId())
                .componentId(entity.getComponentId())
                .componentName(entity.getComponentName())
                .componentVersion(entity.getComponentVersion())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .componentJobs(componentJobs.stream()
                        .map(EntandoBundleComponentJob::fromEntity).collect(Collectors.toList())
                )
                .build();
    }
}
