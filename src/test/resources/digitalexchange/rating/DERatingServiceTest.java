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

package org.entando.kubernetes.digitalexchange.rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.client.digitalexchange.DigitalExchangesClient;
import org.entando.kubernetes.client.digitalexchange.SimpleDigitalExchangeCall;
import org.entando.kubernetes.controller.digitalexchange.rating.DEComponentRatingRequest;
import org.entando.kubernetes.service.digitalexchange.rating.DEComponentRatingResult;
import org.entando.kubernetes.service.digitalexchange.rating.DERatingService;
import org.entando.kubernetes.service.digitalexchange.rating.DERatingsSummary;
import org.entando.web.exception.NotFoundException;
import org.entando.web.response.EntandoEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

@RunWith(MockitoJUnitRunner.class)
public class DERatingServiceTest {

    private static final String COMPONENT_ID = "component_id";
    private static final double AVERAGE_RATING = 4.5;
    private static final int NUMBER_OF_RATINGS = 2;

    private static final String EXCHANGE_ID = "exchangeId";
    private static final int RATING = 5;

    @Mock
    private DigitalExchangesClient client;

    @InjectMocks
    private DERatingService ratingService;

    private static EntandoEntity<DERatingsSummary> getFakeResponse() {
        DERatingsSummary ratingsSummary = new DERatingsSummary();
        ratingsSummary.setComponentId(COMPONENT_ID);
        ratingsSummary.setNumberOfRatings(NUMBER_OF_RATINGS);
        ratingsSummary.setRating(AVERAGE_RATING);

        return new EntandoEntity<>(ratingsSummary);
    }

    @Test
    public void shouldRateComponent() {

        EntandoEntity<DERatingsSummary> response = getFakeResponse();
        when(client.getSingleResponse(eq(EXCHANGE_ID), any())).thenReturn(response);

        DEComponentRatingRequest ratingRequest = getComponentRatingRequest();

        DEComponentRatingResult expectedResult = new DEComponentRatingResult();
        expectedResult.setRatingsSummary(response.getPayload());

        assertThat(ratingService.rateComponent(EXCHANGE_ID, ratingRequest)).isEqualTo(expectedResult);

        ArgumentCaptor<SimpleDigitalExchangeCall<?>> callCaptor
                = ArgumentCaptor.forClass(SimpleDigitalExchangeCall.class);
        verify(client, times(1)).getSingleResponse(eq(EXCHANGE_ID), callCaptor.capture());
        HttpEntity<?> entity = callCaptor.getValue().getEntity();
        assertThat(entity).isNotNull();
        assertThat(entity.getBody()).isEqualTo(ratingRequest);
    }

    @Test
    public void testRatingNotSupported() {

        when(client.getSingleResponse(eq(EXCHANGE_ID), any())).thenAnswer(invocation -> {
            SimpleDigitalExchangeCall<?> call = invocation.getArgument(1);
            RestClientResponseException ex = new HttpServerErrorException(HttpStatus.METHOD_NOT_ALLOWED);
            ReflectionTestUtils.invokeMethod(call, "handleErrorResponse", ex);
            return new EntandoEntity<>(null);
        });

        DEComponentRatingResult result = ratingService.rateComponent(EXCHANGE_ID, getComponentRatingRequest());
        assertThat(result.isRatingSupported()).isEqualTo(false);
    }

    @Test(expected = NotFoundException.class)
    public void testComponentNotFound() {

        when(client.getSingleResponse(eq(EXCHANGE_ID), any())).thenAnswer(invocation -> {
            SimpleDigitalExchangeCall<?> call = invocation.getArgument(1);
            RestClientResponseException ex = new HttpServerErrorException(HttpStatus.NOT_FOUND);
            ReflectionTestUtils.invokeMethod(call, "handleErrorResponse", ex);
            return new EntandoEntity<>(null);
        });

        ratingService.rateComponent(EXCHANGE_ID, getComponentRatingRequest());
    }

    private DEComponentRatingRequest getComponentRatingRequest() {
        DEComponentRatingRequest ratingRequest = new DEComponentRatingRequest();
        ratingRequest.setComponentId(COMPONENT_ID);
        ratingRequest.setRating(RATING);
        return ratingRequest;
    }
}
