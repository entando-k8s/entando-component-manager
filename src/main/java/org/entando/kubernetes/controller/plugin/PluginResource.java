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
package org.entando.kubernetes.controller.plugin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginInfo;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@OpenAPIDefinition(tags = {@Tag(name = "plugins")})
@RequestMapping(value = "/plugins")
public interface PluginResource {

    @Operation(description = "Returns list of linked plugins")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<List<EntandoPlugin>> listLinkedPlugin();

    @Operation(description = "Returns info about linked plugins")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<List<EntandoPluginInfo>> listLinkedPluginInfo();

    @Operation(description = "Returns plugin with given id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @GetMapping(path = "/{pluginId}", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoPlugin> get(@PathVariable final String pluginId);

}
