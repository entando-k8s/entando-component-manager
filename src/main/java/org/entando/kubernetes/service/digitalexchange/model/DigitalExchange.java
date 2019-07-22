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
package org.entando.kubernetes.service.digitalexchange.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@Data
@Validated
@ApiModel
@XmlAccessorType(XmlAccessType.FIELD)
public class DigitalExchange {

    private String id;

    @NotNull(message = "digitalExchange.name.required")
    @Size(min = 3, max = 20, message = "string.size.invalid")
    @ApiModelProperty(required = true)
    private String name;

    @NotNull(message = "digitalExchange.url.required")
    @ApiModelProperty(required = true)
    private String url;

    private int timeout;
    private boolean active;

    private String clientKey;
    private String clientSecret;
    private String publicKey;

    public DigitalExchange() {
    }

    public DigitalExchange(DigitalExchange other) {
        this.id = other.id;
        this.name = other.name;
        this.url = other.url;
        this.timeout = other.timeout;
        this.active = other.active;
        this.clientKey = other.clientKey;
        this.clientSecret = other.clientSecret;
        this.publicKey = other.publicKey;
    }

    public boolean hasNoPublicKey() {
        return StringUtils.isEmpty(this.getPublicKey());
    }

    public void invalidate() {
        this.setPublicKey(null);
        this.setActive(false);
    }
}
