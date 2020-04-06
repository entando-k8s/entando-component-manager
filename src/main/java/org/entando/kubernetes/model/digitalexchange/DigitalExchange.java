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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@NoArgsConstructor
@AllArgsConstructor
public class DigitalExchange {

    private String id;

    @NotNull(message = "digitalExchange.name.required")
    @Size(min = 3, max = 20, message = "string.size.invalid")
    @NotNull
    private String name;

    @NotNull(message = "digitalExchange.url.required")
    private String url;

    private int timeout;
    private boolean active;

    private String clientId;
    private String clientSecret;
    private String publicKey;

}
