
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlansRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/components")
public interface EntandoBundleOperationResource {

    @Operation(description = "Performs a conflict analysis on a Bundle and Version to be installed. "
            + "Then returns an install plan containing every analysed component")
    @ApiResponse(responseCode = "200", description = "Ok")
    @PostMapping(value = "/{component}/installplans", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<InstallPlan>> installPlans(
            @PathVariable("component") String componentId,
            @RequestBody InstallPlansRequest request);

    @Operation(description = "Starts component installation job")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> install(
            @PathVariable("component") String componentId,
            @RequestBody InstallRequest request);

    @Operation(summary = "", description = "Starts component installation job using the InstallPlan", security = {
            @SecurityRequirement(name = "bearerAuth")}, tags = {"entando-bundle-operation-resource-controller"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created")})
    @PutMapping(value = "/{component}/install",
            produces = {"application/json"},
            consumes = {"application/json"})
    ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> installWithInstallPlan(
            @Parameter(in = ParameterIn.PATH, description = "", required = true, schema = @Schema()) @PathVariable("component") String component,
            @Parameter(in = ParameterIn.DEFAULT, description = "", schema = @Schema()) @Valid @RequestBody InstallWithPlansRequest body);


    @Operation(description = "Starts component remove job ")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> uninstall(@PathVariable("component") String componentId,
            HttpServletRequest request) throws URISyntaxException;

    @Operation(description = "Checks installation job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoBundleJobEntity> getLastInstallJob(@PathVariable("component") String componentId);

    @Operation(description = "Checks removal job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoBundleJobEntity> getLastUninstallJob(@PathVariable("component") String componentId);


}
