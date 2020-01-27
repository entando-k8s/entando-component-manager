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
package org.entando.kubernetes.client.digitalexchange;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.entando.kubernetes.model.web.response.RestResponse;
import org.springframework.context.MessageSource;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class DigitalExchangesClientImpl implements DigitalExchangesClient {

    public static final String ERRCODE_DE_HTTP_ERROR = "1";
    public static final String ERRCODE_DE_UNREACHABLE = "2";
    public static final String ERRCODE_DE_TIMEOUT = "3";
    public static final String ERRCODE_DE_INVALID_URL = "4";
    public static final String ERRCODE_DE_WRONG_PAYLOAD = "5";
    public static final String ERRCODE_DE_AUTH = "6";

    private final MessageSource messageSource;
    private final DigitalExchangeRestTemplateFactory restTemplateFactory;

    @Override
    public <R extends RestResponse<?, ?>, C> C getCombinedResult(final List<DigitalExchange> digitalExchanges,
                                                                 final DigitalExchangeCall<R, C> call) {
        final Map<String, R> allResults = queryAllDigitalExchanges(digitalExchanges, call);
        return call.combineResults(allResults);
    }

    private <R extends RestResponse<?, ?>, C> Map<String, R> queryAllDigitalExchanges(final List<DigitalExchange> digitalExchanges,
                                                                                      final DigitalExchangeCall<R, C> call) {
        @SuppressWarnings("unchecked")
        final CompletableFuture<Pair<String, R>>[] futureResults = digitalExchanges
                .stream()
                .filter(DigitalExchange::isActive)
                .map(de -> getSingleResponseAsync(de, call))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futureResults)
                .thenApply(v -> Arrays.stream(futureResults)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).join();
    }

    private <R extends RestResponse<?, ?>, C> CompletableFuture<Pair<String, R>> getSingleResponseAsync(
            DigitalExchange digitalExchange, DigitalExchangeCall<R, C> call) {
        return CompletableFuture.supplyAsync(()
                -> ImmutablePair.of(digitalExchange.getId(), getSingleResponse(digitalExchange, call)));
    }

    @Override
    public <R extends RestResponse<?, ?>, C> R getSingleResponse(DigitalExchange digitalExchange, DigitalExchangeCall<R, C> call) {
        final RestTemplate restTemplate = restTemplateFactory.createRestTemplate(digitalExchange);
        return new DigitalExchangeCallExecutor<>(messageSource, digitalExchange, restTemplate, call).getResponse();
    }

    @Override
    public InputStream getStreamResponse(final DigitalExchange digitalExchange, final DigitalExchangeBaseCall<InputStream> call) {
        final RestTemplate restTemplate = restTemplateFactory.createRestTemplate(digitalExchange);
        return new DigitalExchangeStreamCallExecutor(messageSource, digitalExchange, restTemplate, call).getResponse();
    }

}
