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
package org.entando.kubernetes.model.digitalexchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "digital_exchange_job")
public class DigitalExchangeJob {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "digital_exchange_id")
    private String digitalExchange;

    @Column(name = "component_id")
    private String componentId;

    @Column(name = "component_name")
    private String componentName;

    @Column(name = "component_version")
    private String componentVersion;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "progress")
    private double progress;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

}
