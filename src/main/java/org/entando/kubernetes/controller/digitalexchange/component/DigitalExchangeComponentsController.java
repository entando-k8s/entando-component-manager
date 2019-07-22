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
package org.entando.kubernetes.controller.digitalexchange.component;

import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.model.ResilientPagedMetadata;
import org.entando.web.request.RestListRequest;
import org.entando.web.response.PagedRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeComponentsController implements DigitalExchangeComponentResource {

    private final DigitalExchangeComponentsService componentsService;

    @Override
    public ResponseEntity<PagedRestResponse<DigitalExchangeComponent>> getComponents(RestListRequest requestList) {
//        paginationValidator.validateRestListRequest(requestList, DigitalExchangeComponent.class);
        final ResilientPagedMetadata<DigitalExchangeComponent> resilientPagedMetadata = componentsService.getComponents(requestList);
        final PagedRestResponse<DigitalExchangeComponent> response = new PagedRestResponse<>(resilientPagedMetadata);
        response.setErrors(resilientPagedMetadata.getErrors());
        return ResponseEntity.ok(response);
    }
}
