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
package org.entando.kubernetes.controller.digitalexchange;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.security.Roles;
import org.entando.web.response.RestError;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Api(tags = {"digital-exchange"})
@RequestMapping(value = "/exchanges")
public interface DigitalExchangesResource {

    @ApiOperation(value = "Create a new Digital Exchange configuration")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK")
    })
    @Secured(Roles.WRITE_DIGITAL_EXCHANGE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<DigitalExchange>> create(@Valid @RequestBody DigitalExchange digitalExchange);

    @ApiOperation(value = "Returns a Digital Exchange configuration")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    @Secured(Roles.READ_DIGITAL_EXCHANGE)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<DigitalExchange>> get(@PathVariable("id") String id);

    @ApiOperation(value = "Returns the list of all Digital Exchange configurations")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK")
    })
    @Secured(Roles.READ_DIGITAL_EXCHANGE)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<List<DigitalExchange>>> list();

    @ApiOperation(value = "Update a Digital Exchange configuration")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    @Secured(Roles.WRITE_DIGITAL_EXCHANGE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<DigitalExchange>> update(@PathVariable("id") String id, @Valid @RequestBody DigitalExchange digitalExchange);

    @ApiOperation(value = "Delete a Digital Exchange configuration")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    @Secured(Roles.WRITE_DIGITAL_EXCHANGE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<String>> delete(@PathVariable("id") String id);
    
    @ApiOperation(value = "Test the connection to all Digital Exchange instances")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK")
    })
    @Secured(Roles.READ_DIGITAL_EXCHANGE)
    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<Map<String, List<RestError>>>> testAll();
    
    @ApiOperation(value = "Test the connection to a Digital Exchange instance")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    @Secured(Roles.READ_DIGITAL_EXCHANGE)
    @GetMapping(value = "/test/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<String>> test(@PathVariable("id") String id);
}
