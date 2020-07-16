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
package org.entando.kubernetes.model.digitalexchange;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.apache.commons.compress.utils.Sets;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleSpec;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Entity
@Table(name = "installed_entando_bundles")
public class EntandoBundleEntity {

    @Id
    @NotNull
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;


    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "ecrId", nullable = false )
    private String ecrId;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "code", nullable = false)
    private String code;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "title")
    private String title;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "organization", nullable = false)
    private String organization;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "thumbnail")
    @Convert(converter = ImageConverter.class)
    private String thumbnail;


    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "type")
    @Convert(converter = BundleComponentTypesConverter.class)
    private Set<String> type;

    @OneToOne
    @JoinColumn(name = "last_job_id")
    private EntandoBundleJob lastJob;

    @OneToOne
    @JoinColumn(name = "installed_job_id")
    private EntandoBundleJob installedJob;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }


    public static EntandoBundleEntity newFrom(EntandoComponentBundle bundle) {
        EntandoBundleEntity dec = new EntandoBundleEntity();
        EntandoComponentBundleSpec bspc = bundle.getSpec();
        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        if (bundle.getMetadata().getLabels() != null) {
            bundle.getMetadata().getLabels()
                    .entrySet().stream()
                    .filter(e -> ComponentType.isValidType(e.getKey()))
                    .map(Entry::getKey)
                    .forEach(bundleComponentTypes::add);
        }
        dec.setEcrId(bundle.getMetadata().getName());
        dec.setCode(bspc.getCode());
        dec.setTitle(bspc.getTitle());
        dec.setDescription(bspc.getDescription());
        dec.setThumbnail(bspc.getThumbnail());
        dec.setOrganization(bspc.getOrganization());
        dec.setType(bundleComponentTypes);
        return dec;
    }
}
