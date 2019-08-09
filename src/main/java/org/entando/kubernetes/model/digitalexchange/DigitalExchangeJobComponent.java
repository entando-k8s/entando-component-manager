package org.entando.kubernetes.model.digitalexchange;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.util.UUID;

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
    private ComponentType componentType;

    @Column(name = "name")
    private String name;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "status")
    private JobStatus status;

    // metadata?

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

}
