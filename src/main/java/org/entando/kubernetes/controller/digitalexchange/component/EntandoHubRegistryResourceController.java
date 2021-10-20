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
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.web.response.DeletedObjectResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.entando.kubernetes.validator.EntandoHubRegistryValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntandoHubRegistryResourceController implements EntandoHubRegistryResource {

    private final EntandoHubRegistryService service;
    private final EntandoHubRegistryValidator validator;

    @Autowired
    public EntandoHubRegistryResourceController(EntandoHubRegistryService service,
            EntandoHubRegistryValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    @Override
    public ResponseEntity<SimpleRestResponse<List<EntandoHubRegistry>>> getRegistries() {
        return ResponseEntity.ok(new SimpleRestResponse<>(this.service.listRegistries()));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> addRegistry(
            @RequestBody EntandoHubRegistry entandoHubRegistry) {
        this.validator.validateEntandoHubRegistryOrThrow(entandoHubRegistry, false);
        final EntandoHubRegistry newRegistry = this.service.createRegistry(entandoHubRegistry);
        return new ResponseEntity<>(new SimpleRestResponse<>(newRegistry), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> updateRegistry(
            @RequestBody EntandoHubRegistry entandoHubRegistry) {

        this.validator.validateEntandoHubRegistryOrThrow(entandoHubRegistry,
                EntandoHubRegistryValidator.VALIDATE_ID_TOO);
        final EntandoHubRegistry updatedRegistry = this.service.updateRegistry(entandoHubRegistry);
        return ResponseEntity.ok(new SimpleRestResponse<>(updatedRegistry));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<DeletedObjectResponse>> deleteRegistry(@PathVariable(value = "id") String id) {
        this.service.deleteRegistry(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
