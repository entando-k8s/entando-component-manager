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
package org.entando.kubernetes.controller.digitalexchange.rating;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.rating.DEComponentRatingResult;
import org.entando.kubernetes.service.digitalexchange.rating.DERatingService;
import org.entando.kubernetes.service.digitalexchange.rating.DERatingsSummary;
import org.entando.web.response.RestError;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class DEComponentRatingResourceController implements DEComponentRatingResource {

    private static final String ERROR_CODE_RATING_NOT_SUPPORTED = "2";

    private final @NonNull DERatingService ratingService;

    @Override
    public ResponseEntity<SimpleRestResponse<DERatingsSummary>> rateComponent(@PathVariable("exchange") final String exchangeId,
                                                                         @PathVariable("component") final String componentId,
                                                                         @Valid @RequestBody final DERatingValue rating) {

        final DEComponentRatingRequest ratingRequest = new DEComponentRatingRequest();
        ratingRequest.setComponentId(componentId);
        ratingRequest.setRating(rating.getRating());

        final DEComponentRatingResult result = ratingService.rateComponent(exchangeId, ratingRequest);
        final SimpleRestResponse<DERatingsSummary> response = new SimpleRestResponse<>();

        if (!result.isRatingSupported()) {
            response.addError(new RestError(ERROR_CODE_RATING_NOT_SUPPORTED, "digitalExchange.rating.notSupported"));
            return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
        }

        if (!result.getErrors().isEmpty()) {
            response.setErrors(result.getErrors());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setPayload(result.getRatingsSummary());
        return ResponseEntity.ok(response);
    }
}
