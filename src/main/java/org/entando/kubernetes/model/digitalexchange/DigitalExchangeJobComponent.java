package org.entando.kubernetes.model.digitalexchange;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "digital_exchange_job_component")
public class DigitalExchangeJobComponent {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "digital_exchange_job_id")
    private DigitalExchangeJob job;

    @Column(name = "component_type")
    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    @Column(name = "name")
    private String name;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    // metadata?

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public DigitalExchangeJobComponent duplicate() {
        DigitalExchangeJobComponent newComponent = new DigitalExchangeJobComponent();
        newComponent.setName(getName());
        newComponent.setJob(getJob());
        newComponent.setStatus(getStatus());
        newComponent.setComponentType(getComponentType());
        newComponent.setChecksum(getChecksum());
        newComponent.setErrorMessage(getErrorMessage());
        return newComponent;
    }
}
