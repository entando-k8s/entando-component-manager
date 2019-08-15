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
package org.entando.kubernetes.controller.digitalexchange.category;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.category.DigitalExchangeCategoriesService;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeCategoriesResourceController implements DigitalExchangeCategoriesResource {

    private final @NonNull DigitalExchangeCategoriesService service;

    @Override
    public SimpleRestResponse<List<String>> getCategories() {
        return service.getCategories().toEntity();
    }
}
