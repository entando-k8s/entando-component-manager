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

import lombok.Getter;
import org.entando.web.response.EntandoEntity;
import org.entando.web.response.RestError;

import java.util.ArrayList;
import java.util.List;

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

    public void addValueFromResponse(final EntandoEntity<T> response) {
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            errors.addAll(response.getErrors());
        } else {
            list.add(response.getPayload());
        }
    }

    public EntandoEntity<List<T>> toEntity() {
        final EntandoEntity<List<T>> result = new EntandoEntity<>(getList());
        result.setErrors(getErrors());
        return result;
    }

}
