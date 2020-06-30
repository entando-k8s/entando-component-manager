package org.entando.kubernetes.model.job;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "entando_bundle_component_jobs")
public class EntandoBundleComponentJob {

    @Id
    @Column
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "parent_entando_bundle_job_id")
    private EntandoBundleJob job;

    @Column
    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    @Column
    private String name;

    @Column
    private String errorMessage;

    @Column
    private String checksum;

    @Column
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;
    // metadata?

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public EntandoBundleComponentJob duplicateMetadataWithoutStatus() {
        EntandoBundleComponentJob newComponent = new EntandoBundleComponentJob();
        newComponent.setName(getName());
        newComponent.setJob(getJob());
        newComponent.setComponentType(getComponentType());
        newComponent.setChecksum(getChecksum());
        return newComponent;
    }

    public EntandoBundleComponentJob duplicateMetadataWithStatus() {
        EntandoBundleComponentJob newComponent = this.duplicateMetadataWithoutStatus();
        newComponent.setStatus(this.status);
        newComponent.setErrorMessage(this.errorMessage);
        return newComponent;
    }

    public static EntandoBundleComponentJob create(Installable i, EntandoBundleJob parentJob) {
        EntandoBundleComponentJob component = new EntandoBundleComponentJob();
        component.setJob(parentJob);
        component.setComponentType(i.getComponentType());
        component.setName(i.getName());
        component.setChecksum(i.getChecksum());
        return component;
    }

}
