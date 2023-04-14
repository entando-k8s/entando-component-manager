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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.PluginRolesConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "plugin_data")
public class PluginDataEntity {

    @Id
    @Column
    //@Type(type = "uuid-char")
    @JdbcTypeCode(Types.VARCHAR)
    private UUID id;

    @NotNull
    @Column(name = "bundle_id", nullable = false)
    private String bundleId;

    @NotNull
    @Column(name = "plugin_name", nullable = false)
    private String pluginName;

    @NotNull
    @Column(name = "plugin_code", nullable = false)
    private String pluginCode;

    @NotNull
    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "custom_endpoint")
    private String customEndpoint;

    @Column(name = "roles")
    @Convert(converter = PluginRolesConverter.class)
    private Set<String> roles = new HashSet<>();

    @Size(min = 1, max = 64)
    @Column(name = "docker_tag")
    private String dockerTag;

    @Size(min = 71, max = 71)
    @Column(name = "docker_sha256")
    private String dockerSha256;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }
}
