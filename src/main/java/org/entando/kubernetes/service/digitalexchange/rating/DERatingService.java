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
package org.entando.kubernetes.service.digitalexchange.rating;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.rating.DEComponentRatingRequest;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangesService;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.client.SimpleDigitalExchangeCall;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.web.exception.NotFoundException;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DERatingService {

    private final @NonNull DigitalExchangesClient client;
    private final @NonNull DigitalExchangesService digitalExchangesService;

    public DEComponentRatingResult rateComponent(final String exchangeId, final DEComponentRatingRequest ratingRequest) {
        final SimpleDigitalExchangeCall<DERatingsSummary> call = new SimpleDigitalExchangeCall<>(
                HttpMethod.POST, new ParameterizedTypeReference<SimpleRestResponse<DERatingsSummary>>() {
        }, "digitalExchange", "components", ratingRequest.getComponentId(), "rate");
        final DEComponentRatingResult result = new DEComponentRatingResult();

        call.setEntity(new HttpEntity<>(ratingRequest));
        call.setErrorResponseHandler(ex -> {
            int status = ex.getRawStatusCode();
            if (status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                result.setRatingUnsupported();
            } else if (status == HttpStatus.NOT_FOUND.value()) {
                throw new NotFoundException("component");
//                throw new NotFoundException("component", ratingRequest.getComponentId());
            }
            return Optional.empty();
        });

        final DigitalExchange digitalExchange = digitalExchangesService.findById(exchangeId);
        final SimpleRestResponse<DERatingsSummary> response = client.getSingleResponse(digitalExchange, call);

        if (result.isRatingSupported()) {
            result.setRatingsSummary(response.getPayload());
        }
        result.setErrors(response.getErrors());

        return result;
    }
}
