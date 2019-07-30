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
package org.entando.kubernetes.service.digitalexchange.category;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangesService;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.client.SimpleDigitalExchangeCall;
import org.entando.kubernetes.service.digitalexchange.model.ResilientListWrapper;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeCategoriesServiceImpl implements DigitalExchangeCategoriesService {

    private static final ParameterizedTypeReference<SimpleRestResponse<List<String>>> TYPE_REFERENCE =
            new ParameterizedTypeReference<SimpleRestResponse<List<String>>>() {};

    private final @NonNull DigitalExchangesClient client;
    private final @NonNull DigitalExchangesService digitalExchangesService;

    @Override
    public ResilientListWrapper<String> getCategories() {
        final ResilientListWrapper<String> result = new ResilientListWrapper<>();
        final SimpleDigitalExchangeCall<List<String>> call = new SimpleDigitalExchangeCall<>(HttpMethod.GET,
                TYPE_REFERENCE, "digitalExchange", "categories");
        final ResilientListWrapper<List<String>> responses = client.getCombinedResult(digitalExchangesService.getDigitalExchanges(), call);
        final Set<String> categories = responses.getList().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        result.getList().addAll(categories);
        result.getErrors().addAll(responses.getErrors());
        
        return result;
    }

}
