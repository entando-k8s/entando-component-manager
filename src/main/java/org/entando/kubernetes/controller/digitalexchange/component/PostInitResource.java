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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/post-inits")
public interface PostInitResource {

    static final String VERSION = "v1";
    static final String ACCEPTED_MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE + ";v=" + VERSION;

    @Operation(description = "Returns post init installation status")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "500", description = "Generic server error")
    @GetMapping(value = "/status", produces = ACCEPTED_MEDIA_TYPE)
    ResponseEntity<String> getStatus();

}