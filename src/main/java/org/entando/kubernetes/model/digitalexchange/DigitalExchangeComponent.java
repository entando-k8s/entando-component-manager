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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.entando.kubernetes.model.web.SystemConstants;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class DigitalExchangeComponent {

    @NotNull
    @Size(min = 1, max = 30)
    private String id;

    @NotNull
    @Size(min = 1, max = 30)
    private String name;

    @NotNull
    @Size(min = 1, max = 30)
    private String type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SystemConstants.API_DATE_FORMAT)
    private Date lastUpdate;

    private String version;
    private String description;
    private String image;
    private double rating;
    private boolean installed;
    private String digitalExchangeName;

    // NOTE: the id can be removed when we will return the installation link
    // following the HATEOAS principle
    private String digitalExchangeId;

    private String signature;

    private Map<String, String> metadata;

}
