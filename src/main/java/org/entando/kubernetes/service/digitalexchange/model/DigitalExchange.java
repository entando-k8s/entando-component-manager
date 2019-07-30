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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@Validated
@ApiModel
@NoArgsConstructor
@AllArgsConstructor
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

}
