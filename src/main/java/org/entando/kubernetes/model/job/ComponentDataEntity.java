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

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.hibernate.annotations.Type;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "component_data")
public class ComponentDataEntity {

    @Id
    @Column
    @Type(type = "uuid-char")
    private UUID id;

    @NotNull
    @Column(name = "bundle_id", nullable = false)
    private String bundleId;

    @NotNull
    @Column(name = "component_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    @Column(name = "component_sub_type")
    private String componentSubType;

    @Column(name = "component_id")
    private String componentId;

    @Column(name = "component_name")
    private String componentName;

    @Column(name = "component_code")
    private String componentCode;

    @Column(name = "component_group")
    private String componentGroup;

    @Column(name = "component_descriptor")
    private String componentDescriptor;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    /******************************************************
     * TRANSIENT FIELDS.
     *****************************************************/

    @Transient
    private String bundleCode;
}
