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
import org.entando.kubernetes.model.web.SystemConstants;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Entity
@Table(name = "digital_exchange_installed_bundles")
public class DigitalExchangeComponent {

    @NotNull
    @Id
    private UUID id;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(unique = true, nullable = false)
    private String bundleId;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 30)
    private String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SystemConstants.API_DATE_FORMAT)
    @Column
    private Date lastUpdate;

    @Column(nullable = false)
    private String version;

    private String description;

    private String image;

    private double rating;

    private boolean installed;

    @Column(nullable = false)
    private String digitalExchangeName;

    // NOTE: the id can be removed when we will return the installation link
    // following the HATEOAS principle

    @Column(nullable = false)
    private String digitalExchangeId;

    private String signature;

    @Convert(converter = HashMapConverter.class)
    private Map<String, String> metadata;

    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID();
    }

}
