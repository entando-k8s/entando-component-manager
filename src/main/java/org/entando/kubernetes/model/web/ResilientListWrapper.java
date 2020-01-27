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
package org.entando.kubernetes.model.web;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.entando.kubernetes.model.web.response.RestError;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;

/**
 * Wrapper for handling a sequence of responses with partial failure.
 */
@Getter
public class ResilientListWrapper<T> {

    private final List<T> list;
    private final List<RestError> errors;

    public ResilientListWrapper() {
        list = new ArrayList<>();
        errors = new ArrayList<>();
    }

    public void addValueFromResponse(final SimpleRestResponse<T> response) {
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            errors.addAll(response.getErrors());
        } else {
            list.add(response.getPayload());
        }
    }

    public SimpleRestResponse<List<T>> toEntity() {
        final SimpleRestResponse<List<T>> result = new SimpleRestResponse<>(getList());
        result.setErrors(getErrors());
        return result;
    }

}
