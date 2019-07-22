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

import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangesService;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.web.response.EntandoEntity;
import org.entando.web.response.RestError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DigitalExchangesResourceController implements DigitalExchangesResource {

    private final DigitalExchangesService digitalExchangeService;

    @Override
    public ResponseEntity<EntandoEntity<List<DigitalExchange>>> list() {
        return ResponseEntity.ok(new EntandoEntity<>(digitalExchangeService.getDigitalExchanges()));
    }

    @Override
    public ResponseEntity<EntandoEntity<DigitalExchange>> create(@Valid @RequestBody DigitalExchange digitalExchange) {
        final DigitalExchange de = digitalExchangeService.create(digitalExchange);
        final EntandoEntity<DigitalExchange> response = new EntandoEntity<>(de);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EntandoEntity<DigitalExchange>> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(new EntandoEntity<>(digitalExchangeService.findById(id)));
    }

    @Override
    public ResponseEntity<EntandoEntity<DigitalExchange>> update(@PathVariable("id") String id, @Valid @RequestBody DigitalExchange digitalExchange) {
        digitalExchange.setId(id);
        final DigitalExchange de = digitalExchangeService.update(digitalExchange);
        return ResponseEntity.ok(new EntandoEntity<>(de));
    }

    @Override
    public ResponseEntity<EntandoEntity<String>> delete(@PathVariable("id") String id) {
        digitalExchangeService.delete(id);
        return ResponseEntity.ok(new EntandoEntity<>(id));
    }

    @Override
    public ResponseEntity<EntandoEntity<Map<String, List<RestError>>>> testAll() {
        return ResponseEntity.ok(new EntandoEntity<>(digitalExchangeService.testAll()));
    }

    @Override
    public ResponseEntity<EntandoEntity<String>> test(@PathVariable("id") String id) {
        final List<RestError> errors = digitalExchangeService.test(id);
        final EntandoEntity<String> response = new EntandoEntity<>(errors.isEmpty() ? "OK" : "");
        response.setErrors(errors);

        return ResponseEntity.ok(response);
    }
}
