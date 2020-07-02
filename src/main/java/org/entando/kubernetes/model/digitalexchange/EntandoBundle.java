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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.apache.commons.compress.utils.Sets;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.web.SystemConstants;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Entity
@Table(name = "installed_entando_bundles")
public class EntandoBundle {


    @Id
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "type")
    @Convert(converter = BundleComponentTypesConverter.class)
    private Set<String> type;

    @OneToOne
    @JoinColumn(name = "job_id")
    private EntandoBundleJob job;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SystemConstants.API_DATE_FORMAT)
    @Column(name = "last_update")
    private Date lastUpdate;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "image")
    @Convert(converter = ImageConverter.class)
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

    public static EntandoBundle newFrom(EntandoDeBundle bundle) {
        EntandoBundle dec = new EntandoBundle();
        String bundleId = bundle.getMetadata().getName();
        EntandoDeBundleDetails bd = bundle.getSpec().getDetails();
        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        if (bundle.getMetadata().getLabels() != null) {
            bundle.getMetadata().getLabels()
                    .entrySet().stream()
                    .filter(e -> ComponentType.isValidType(e.getKey()))
                    .map(Entry::getKey)
                    .forEach(bundleComponentTypes::add);
        }
        dec.setId(bundleId);
        dec.setName(bundle.getSpec().getDetails().getName());
        dec.setDescription(bd.getDescription());
        dec.setRating(5);
        dec.setType(bundleComponentTypes);
        dec.setLastUpdate(new Date());
        dec.setSignature("");
        dec.setInstalled(false);
        dec.setVersion(bd.getDistTags().get("latest").toString());
        dec.setImage(bd.getThumbnail());
        return dec;
    }
}
