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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.web.response.DeletedObjectResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/registries")
public interface EntandoHubRegistryResource {

    @Operation(description = "Returns available Entando Hub registries")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<List<EntandoHubRegistry>>> getRegistries();

    @Operation(description = "Add new Entando Hub registry")
    @ApiResponse(responseCode = "200", description = "OK")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> addRegistry(@RequestBody EntandoHubRegistry entandoHubRegistry);

    @Operation(description = "Update an Entando Hub registry")
    @ApiResponse(responseCode = "200", description = "OK")
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> updateRegistry(@RequestBody EntandoHubRegistry entandoHubRegistry);

    @Operation(description = "Delete an Entando Hub registry")
    @ApiResponse(responseCode = "200", description = "OK")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<DeletedObjectResponse>> deleteRegistry(@PathVariable("id") String id);
}
