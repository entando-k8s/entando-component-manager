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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeComponentsController implements DigitalExchangeComponentResource {

    private final DigitalExchangeComponentsService componentsService;

    @Override
    public ResponseEntity<PagedRestResponse<DigitalExchangeComponent>> getComponents() {
        List<DigitalExchangeComponent>  bundles = componentsService.getComponents();
        PagedMetadata<DigitalExchangeComponent> pagedMetadata =
                new PagedMetadata<>(1, 100, 1, bundles.size());
        pagedMetadata.setBody(bundles);
        PagedRestResponse<DigitalExchangeComponent> response = new PagedRestResponse(pagedMetadata);
        return ResponseEntity.ok(response);
    }
}
