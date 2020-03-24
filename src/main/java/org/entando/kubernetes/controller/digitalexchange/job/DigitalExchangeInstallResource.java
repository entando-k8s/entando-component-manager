
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
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@OpenAPIDefinition(tags = {@Tag(name = "digital-exchange"), @Tag(name = "installation")})
@RequestMapping(value = "/components")
public interface DigitalExchangeInstallResource {

    @Operation(description = "Starts component installation job")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<DigitalExchangeJob> install(
            @PathVariable("component") String componentId,
            @RequestParam(name = "version", required = true, defaultValue = "latest") String version);

    @Operation(description = "Starts component remove job ")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<DigitalExchangeJob> uninstall(@PathVariable("component") String componentId,
            HttpServletRequest request) throws URISyntaxException;

    @Operation(description = "Checks installation job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/install", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<DigitalExchangeJob> getLastInstallJob(@PathVariable("component") String componentId);

    @Operation(description = "Checks removal job status")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/uninstall", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<DigitalExchangeJob> getLastUninstallJob(@PathVariable("component") String componentId);


}
