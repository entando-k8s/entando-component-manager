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
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.entando.kubernetes.validator.EntandoHubRegistryValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntandoHubRegistryResourceController implements EntandoHubRegistryResource {

    private final EntandoHubRegistryService service;
    private final EntandoHubRegistryValidator validator;
    private final AuthorizationChecker authorizationChecker;

    @Autowired
    public EntandoHubRegistryResourceController(EntandoHubRegistryService service,
            EntandoHubRegistryValidator validator,
            AuthorizationChecker authorizationChecker) {

        this.service = service;
        this.validator = validator;
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public ResponseEntity<SimpleRestResponse<List<EntandoHubRegistry>>> getRegistries() {
        return ResponseEntity.ok(new SimpleRestResponse<>(this.service.listRegistries()));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> addRegistry(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody EntandoHubRegistry entandoHubRegistry) {

        this.authorizationChecker.checkPermissions(authorizationHeader);
        this.validator.validateEntandoHubRegistryOrThrow(entandoHubRegistry, false);
        final EntandoHubRegistry newRegistry = this.service.createRegistry(entandoHubRegistry);
        return new ResponseEntity<>(new SimpleRestResponse<>(newRegistry), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> updateRegistry(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody EntandoHubRegistry entandoHubRegistry) {

        this.authorizationChecker.checkPermissions(authorizationHeader);
        this.validator.validateEntandoHubRegistryOrThrow(entandoHubRegistry,
                EntandoHubRegistryValidator.VALIDATE_ID_TOO);
        final EntandoHubRegistry updatedRegistry = this.service.updateRegistry(entandoHubRegistry);
        return ResponseEntity.ok(new SimpleRestResponse<>(updatedRegistry));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<DeletedObjectResponse>> deleteRegistry(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable(value = "id") String id) {

        this.authorizationChecker.checkPermissions(authorizationHeader);
        final String deleteRegistryName = this.service.deleteRegistry(id);
        return ResponseEntity.ok(new SimpleRestResponse<>(new DeletedObjectResponse(deleteRegistryName)));
    }
}
