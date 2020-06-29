
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
package org.entando.kubernetes.controller.digitalexchange.job;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

@OpenAPIDefinition(tags = {@Tag(name = "digital-exchange"), @Tag(name = "installation")})
@RequestMapping(value = "/components")
public interface EntandoBundleOperationResource {

    @Operation(description = "Starts component installation job")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundleJob>> install(
            @PathVariable("component") String componentId,
            @RequestParam(name = "version", required = true, defaultValue = "latest") String version);

    @Operation(description = "Starts component remove job ")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundleJob>> uninstall(@PathVariable("component") String componentId,
            HttpServletRequest request) throws URISyntaxException;

    @Operation(description = "Checks installation job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoBundleJob> getLastInstallJob(@PathVariable("component") String componentId);

    @Operation(description = "Checks removal job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoBundleJob> getLastUninstallJob(@PathVariable("component") String componentId);


}
