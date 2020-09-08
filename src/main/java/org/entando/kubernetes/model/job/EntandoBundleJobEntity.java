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

package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.installable.Installable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "entando_bundle_jobs")
public class EntandoBundleJobEntity implements TrackableJob, HasProgress {

    @Id
    @Column
    private UUID id;

    @Column
    private String componentId;

    @Column
    private String componentName;

    @Column
    private String componentVersion;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column
    private String userId;

    @Column
    private double progress;

    @Column
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column
    private String errorMessage;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public EntandoBundleJobEntity clone() {
        EntandoBundleJobEntity newEntity = new EntandoBundleJobEntity();
        newEntity.setId(this.id);
        newEntity.setProgress(this.progress);
        newEntity.setStatus(this.status);
        newEntity.setComponentName(this.componentName);
        newEntity.setComponentId(this.componentId);
        newEntity.setComponentVersion(this.componentVersion);
        newEntity.setErrorMessage(this.errorMessage);
        newEntity.setStartedAt(this.startedAt);
        newEntity.setFinishedAt(this.finishedAt);
        newEntity.setUserId(this.userId);
        return newEntity;
    }

}
