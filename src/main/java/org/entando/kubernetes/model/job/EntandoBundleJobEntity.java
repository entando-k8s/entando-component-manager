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

import static org.entando.kubernetes.model.bundle.installable.Installable.MAX_COMMON_SIZE_OF_STRINGS;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "entando_bundle_jobs")
public class EntandoBundleJobEntity implements TrackableJob, HasProgress {

    @Id
    @Column
    @Type(type = "uuid-char")
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
    private Integer installErrorCode;
    @Column
    private String installErrorMessage;
    @Column
    private Integer rollbackErrorCode;
    @Column
    private String rollbackErrorMessage;
    @Column
    private String installPlan;
    /**
     * this field denotes if a bundle installation has been customized by the user. a bundle installation becomes custom
     * when the bundle is not installed entirely (one or more components are skipped or overridden)
     */
    @Column
    private Boolean customInstallation;
    @Column
    private Integer uninstallErrorCode;
    @Column
    private String uninstallErrorMessage;
    @Column
    @Size(max = MAX_COMMON_SIZE_OF_STRINGS)
    private String uninstallErrors;
    @Column
    @Size(max = MAX_COMMON_SIZE_OF_STRINGS)
    private String installWarnings;

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
        newEntity.setInstallErrorCode(this.installErrorCode);
        newEntity.setInstallErrorMessage(this.installErrorMessage);
        newEntity.setRollbackErrorCode(this.rollbackErrorCode);
        newEntity.setRollbackErrorMessage(this.rollbackErrorMessage);
        newEntity.setStartedAt(this.startedAt);
        newEntity.setFinishedAt(this.finishedAt);
        newEntity.setUserId(this.userId);
        newEntity.setInstallPlan(this.installPlan);
        newEntity.setCustomInstallation(this.customInstallation);
        newEntity.setUninstallErrorCode(this.uninstallErrorCode);
        newEntity.setUninstallErrorMessage(this.uninstallErrorMessage);
        newEntity.setUninstallErrors(this.uninstallErrors);
        newEntity.setInstallWarnings(this.installWarnings);
        return newEntity;
    }
}
