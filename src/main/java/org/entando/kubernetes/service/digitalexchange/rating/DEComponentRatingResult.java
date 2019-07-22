/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.kubernetes.service.digitalexchange.rating;

import lombok.Data;
import org.entando.web.response.RestError;

import java.util.ArrayList;
import java.util.List;

@Data
public class DEComponentRatingResult {

    private boolean ratingSupported = true;
    private DERatingsSummary ratingsSummary;
    private List<RestError> errors = new ArrayList<>();

    public void setRatingUnsupported() {
        this.ratingSupported = false;
    }

}
