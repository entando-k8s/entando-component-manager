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
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/components")
public interface EntandoBundleResource {

    @Operation(description = "Returns available Digital Exchange components")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedRestResponse<EntandoBundle>> getBundles(PagedListRequest requestList);

    @Operation(description = "Returns a single Digital Exchange component")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundle>> getBundleByRestNamedId(@PathVariable RestNamedId restNamedId);

    @Operation(description = "Deploy to Kubernetes a new EntandoDeBundle")
    @ApiResponse(responseCode = "200", description = "OK")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<EntandoBundle>> deployBundle(@RequestBody BundleInfo bundleInfo);

    @Operation(description = "Return bundle components in use")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/{component}/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<List<EntandoCoreComponentUsage>>> getBundleUsageSummary(
            @PathVariable("component") String component);

    @Operation(description = "Return the status of the available bundles")
    @ApiResponse(responseCode = "200", description = "OK")
    @PostMapping(value = "/status/query", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<BundlesStatusResult>> getBundlesStatus(
            @RequestBody BundlesStatusQuery bundlesStatusQuery);
}
