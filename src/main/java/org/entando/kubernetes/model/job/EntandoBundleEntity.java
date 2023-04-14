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

import static org.entando.kubernetes.model.bundle.installable.Installable.MAX_COMMON_SIZE_OF_STRINGS;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Types;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.BundleComponentTypesConverter;
import org.entando.kubernetes.model.web.SystemConstants;
import org.hibernate.annotations.JdbcTypeCode;
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
    //@Type(type = "uuid-char")
    @JdbcTypeCode(Types.VARCHAR)
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

    @Size(max = MAX_COMMON_SIZE_OF_STRINGS)
    @Column(name = "ext")
    private String ext;

    @Size(min = 71, max = 71)
    @Column(name = "image_digest")
    private String imageDigest;


    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }
}
