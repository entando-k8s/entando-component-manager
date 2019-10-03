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
package org.entando.kubernetes.service.digitalexchange.component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangesService;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.client.SimpleDigitalExchangeCall;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.model.ResilientPagedMetadata;
import org.entando.web.request.Filter;
import org.entando.web.request.PagedListRequest;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DigitalExchangeComponentsServiceImpl implements DigitalExchangeComponentsService {

    private static final List<String> LOCAL_FILTERS = Arrays.asList("digitalExchangeName", "digitalExchangeId", "installed");

    private final @NonNull DigitalExchangesClient client;
    private final @NonNull DigitalExchangesService exchangesService;
    private final @NonNull KubernetesService kubernetesService;

    @Override
    public ResilientPagedMetadata<DigitalExchangeComponent> getComponents(final PagedListRequest requestList) {
        final ResilientPagedMetadata<DigitalExchangeComponent> combinedResult = client.getCombinedResult(
                exchangesService.getDigitalExchanges(),
                new ComponentsCall(exchangesService, buildForwardedRequest(requestList)));
        final List<DigitalExchangeComponent> localFilteredList = new DigitalExchangeComponentListProcessor(
                requestList, combinedResult.getBody()).filterAndSort().toList();
        final List<DigitalExchangeComponent> sublist = requestList.getSublist(localFilteredList);
        sublist.forEach(this::processInstalled);

        combinedResult.setTotalItems(localFilteredList.size());
        combinedResult.setBody(sublist);
        combinedResult.setPage(requestList.getPage());
        combinedResult.setPageSize(requestList.getPageSize());

        return combinedResult;
    }

    @Override
    public SimpleRestResponse<DigitalExchangeComponent> getComponent(final DigitalExchange digitalExchange, final String componentId) {
        final SimpleDigitalExchangeCall<DigitalExchangeComponent> call = new SimpleDigitalExchangeCall<>(
                HttpMethod.GET, new ParameterizedTypeReference<SimpleRestResponse<DigitalExchangeComponent>>() {
        }, "digitalExchange", "components", componentId);
        final SimpleRestResponse<DigitalExchangeComponent> response = client.getSingleResponse(digitalExchange, call);
        processInstalled(response.getPayload());
        return response;
    }

    private void processInstalled(final DigitalExchangeComponent component) {
        kubernetesService.getPluginOptional(component.getId()).ifPresent(entandoPlugin -> component.setInstalled(true));
//        kubernetesService.getDeploymentOptional(component.getId())
//            .ifPresent(deployment -> component.setInstalled(true));
    }

    private PagedListRequest buildForwardedRequest(final PagedListRequest originalRequest) {
        final PagedListRequest forwaredRequest = new PagedListRequest();
        forwaredRequest.setDirection(originalRequest.getDirection());
        forwaredRequest.setSort(originalRequest.getSort());
        forwaredRequest.setPageSize(Integer.MAX_VALUE);
        forwaredRequest.setPage(1);

        if (originalRequest.getFilters() != null) {
            final Filter[] forwaredFilters = Arrays.stream(originalRequest.getFilters())
                    .filter(f -> !LOCAL_FILTERS.contains(f.getAttribute()))
                    .toArray(Filter[]::new);
            forwaredRequest.setFilters(forwaredFilters);
        }

        return forwaredRequest;
    }
}
