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
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.web.SystemConstants;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Entity
@Table(name = "digital_exchange_installed_components")
public class DigitalExchangeComponent {

    @NotNull
    @Id
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name="component_id", unique = true, nullable = false)
    private String componentId;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name="name", nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name="type")
    private String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SystemConstants.API_DATE_FORMAT)
    @Column(name="last_update")
    private Date lastUpdate;

    @Column(name="version", nullable = false)
    private String version;

    @Column(name="description")
    private String description;

    @Column(name="image")
    private String image;

    @Column(name="rating")
    private double rating;

    @Column(name="installed")
    private boolean installed;

    @Column(name="digital_exchange_name", nullable = false)
    private String digitalExchangeName;

    // NOTE: the id can be removed when we will return the installation link
    // following the HATEOAS principle

    @Column(name="digital_exchange_id", nullable = false)
    private String digitalExchangeId;

    @Column(name="signature")
    private String signature;

    @Convert(converter = HashMapConverter.class)
    @Column(name="metadata")
    private Map<String, String> metadata;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

    public static DigitalExchangeComponent newFrom(EntandoDeBundle bundle) {
        DigitalExchangeComponent dec = new DigitalExchangeComponent();
        String bundleId = bundle.getMetadata().getName();
        EntandoDeBundleDetails bd = bundle.getSpec().getDetails();
        dec.setComponentId(bundleId);
        dec.setName(bundle.getSpec().getDetails().getName());
        dec.setDescription(bd.getDescription());
        dec.setDigitalExchangeId(bundle.getMetadata().getNamespace());
        dec.setDigitalExchangeName(bundle.getMetadata().getNamespace());
        dec.setRating(5);
        dec.setType("Bundle");
        dec.setLastUpdate(new Date());
        dec.setSignature("");
        dec.setVersion(bd.getDistTags().get("latest").toString());
        dec.setImage(bd.getThumbnail());
        return dec;
    }
}
