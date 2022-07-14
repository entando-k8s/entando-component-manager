/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.BundleComponentTypesConverter;
import org.entando.kubernetes.model.web.SystemConstants;
import org.hibernate.annotations.Type;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "installed_entando_bundles")
public class EntandoBundleEntity {

    @Id
    @Type(type = "uuid-char")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "bundle_code", unique = true, nullable = false)
    private String bundleCode;

    @NotNull
    @Column(name = "bundleType", nullable = false)
    private String bundleType;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "type")
    @Convert(converter = BundleComponentTypesConverter.class)
    private Set<String> type;

    @OneToOne
    @JoinColumn(name = "job_id")
    private EntandoBundleJobEntity job;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SystemConstants.API_DATE_FORMAT)
    @Column(name = "last_update")
    private Date lastUpdate;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "description")
    private String description;

    @Column(name = "image")
    private String image;

    @Column(name = "rating")
    private double rating;

    @Column(name = "installed")
    private boolean installed;

    @Column(name = "signature")
    private String signature;

    @Convert(converter = HashMapConverter.class)
    @Column(name = "metadata")
    private Map<String, String> metadata;

    @Size(max = 511)
    @Column(name = "repo_url")
    private String repoUrl;

    @Size(max = 1023)
    @Column(name = "pbc_list")
    private String pbcList;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }
}
