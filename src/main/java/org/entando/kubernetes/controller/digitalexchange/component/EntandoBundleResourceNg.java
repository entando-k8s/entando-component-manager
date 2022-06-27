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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.entando.kubernetes.model.bundle.EntandoBundleData;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/bundles")
public interface EntandoBundleResourceNg {

    static final String REPO_URL_PATH_PARAM = "url";
    static final String VERSION = "v1";
    static final String ACCEPTED_MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE + ";v=" + VERSION;

    @Operation(description = "Returns available bundles")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Generic server error")
    @GetMapping(produces = ACCEPTED_MEDIA_TYPE)
    ResponseEntity<PagedRestResponse<EntandoBundleData>> getBundles(PagedListRequest requestList);

    @Operation(description = "Returns available installed Digital Exchange plugin components for a bundle specified by bundleId or by url encoded")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not valid input in URL or Bundle not installed")
    @ApiResponse(responseCode = "406", description = "Not valid media type ")
    @ApiResponse(responseCode = "422", description = "bundleId/url not compliant")
    @ApiResponse(responseCode = "500", description = "Generic server error")
    @GetMapping(value = "/{bundleId}/plugins", produces = ACCEPTED_MEDIA_TYPE)
    ResponseEntity<PagedRestResponse<PluginData>> getBundleInstalledComponents(
            @PathVariable("bundleId") @Parameter(description = "Formatted as: bundleId | `" + REPO_URL_PATH_PARAM
                    + "=<BASE64_URL>`",
                    schema = @Schema(implementation = String.class)) RestNamedId bundleId,
            PagedListRequest requestList);

    @Operation(description = "Returns available installed Digital Exchange plugin component specified by bundleId or by url encoded and pluginCode")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not valid input in URL or Bundle not installed")
    @ApiResponse(responseCode = "406", description = "Not valid media type ")
    @ApiResponse(responseCode = "422", description = "bundleId/url not compliant")
    @ApiResponse(responseCode = "500", description = "Generic server error")
    @GetMapping(value = "/{bundleId}/plugins/{pluginName}", produces = ACCEPTED_MEDIA_TYPE)
    ResponseEntity<PluginData> getBundleInstalledPlugin(
            @PathVariable("bundleId") @Parameter(description = "Formatted as: bundleId | `" + REPO_URL_PATH_PARAM
                    + "=<BASE64_URL>`",
                    schema = @Schema(implementation = String.class)) RestNamedId bundleId,
            @PathVariable("pluginName") @Parameter(description = "Formatted as: pluginName",
                    schema = @Schema(implementation = String.class)) String pluginName);

}