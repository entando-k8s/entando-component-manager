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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.entando.kubernetes.model.bundle.EntandoBundleUsageSummary;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@OpenAPIDefinition(tags = {@Tag(name = "digital-exchange"), @Tag(name = "components")})
@RequestMapping(value = "/components")
public interface DigitalExchangeComponentResource {

    @Operation(description = "Returns available Digital Exchange components")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedRestResponse<DigitalExchangeComponent>> getComponents();

    @Operation(description = "Return bundle components in use")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/usage", produces =  MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntandoBundleUsageSummary> getUsageSummary(@PathVariable("component") String component);
}
