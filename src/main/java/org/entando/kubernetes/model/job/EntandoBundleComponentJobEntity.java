package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.hibernate.annotations.Type;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "entando_bundle_component_jobs")
public class EntandoBundleComponentJobEntity implements TrackableJob, HasInstallable {

    @Transient
    Installable installable;
    @Transient
    private String uninstallErrors;
    @Transient
    private String installWarnings;

    @Id
    @Column
    @Type(type = "uuid-char")
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "parent_entando_bundle_job_id")
    private EntandoBundleJobEntity parentJob;
    @Column
    @Enumerated(EnumType.STRING)
    private ComponentType componentType;
    @Column
    private String componentId;
    @Column
    private Integer installErrorCode;
    @Column
    private String installErrorMessage;
    @Column
    private Integer uninstallErrorCode;
    @Column
    private String uninstallErrorMessage;
    @Column
    private Integer rollbackErrorCode;
    @Column
    private String rollbackErrorMessage;
    @Column
    private String checksum;
    @Column
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @Column
    @Enumerated(EnumType.STRING)
    private InstallAction action;
    @Column
    private LocalDateTime startedAt;
    // metadata?
    @Column
    private LocalDateTime finishedAt;

    public static EntandoBundleComponentJobEntity getNewCopy(EntandoBundleComponentJobEntity o) {
        EntandoBundleComponentJobEntity newComponent = new EntandoBundleComponentJobEntity();
        newComponent.setParentJob(o.getParentJob());
        newComponent.setComponentType(o.getComponentType());
        newComponent.setComponentId(o.getComponentId());
        newComponent.setChecksum(o.getChecksum());
        newComponent.setStartedAt(o.getStartedAt());
        newComponent.setFinishedAt(o.getFinishedAt());
        newComponent.setStatus(o.getStatus());
        newComponent.setAction(o.getAction());
        newComponent.setInstallable(o.getInstallable());
        newComponent.setInstallErrorMessage(o.getInstallErrorMessage());
        newComponent.setInstallErrorCode(o.getInstallErrorCode());
        newComponent.setUninstallErrorMessage(o.getUninstallErrorMessage());
        newComponent.setUninstallErrorCode(o.getUninstallErrorCode());
        newComponent.setRollbackErrorMessage(o.getRollbackErrorMessage());
        newComponent.setRollbackErrorCode(o.getRollbackErrorCode());
        return newComponent;
    }

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

}
