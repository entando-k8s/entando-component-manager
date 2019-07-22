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
package org.entando.kubernetes.service.digitalexchange.component;

import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangesService;
import org.entando.kubernetes.service.digitalexchange.client.PagedDigitalExchangeCall;
import org.entando.web.request.Filter;
import org.entando.web.request.FilterOperator;
import org.entando.web.request.RequestListProcessor;
import org.entando.web.request.RestListRequest;
import org.entando.web.response.PagedRestResponse;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

public class ComponentsCall extends PagedDigitalExchangeCall<DigitalExchangeComponent> {

    private final DigitalExchangesService exchangesService;

    public ComponentsCall(DigitalExchangesService exchangesService, RestListRequest requestList) {
        super(requestList, new ParameterizedTypeReference<PagedRestResponse<DigitalExchangeComponent>>() {
        }, "digitalExchange", "components");
        this.exchangesService = exchangesService;
    }

    public ComponentsCall(DigitalExchangesService exchangesService, RestListRequest requestList, String componentType) {
        this(exchangesService, filterByType(requestList, componentType));
    }

    @Override
    protected void preprocessResponse(String exchangeId, PagedRestResponse<DigitalExchangeComponent> response) {
        if (response.getErrors().isEmpty()) {
            String exchangeName = exchangesService.findById(exchangeId).getName();
            response.getPayload().forEach(de -> {
                de.setDigitalExchangeName(exchangeName);
                de.setDigitalExchangeId(exchangeId);
            });
        }
    }

    @Override
    protected RequestListProcessor<DigitalExchangeComponent> getRequestListProcessor(RestListRequest request, List<DigitalExchangeComponent> joinedList) {
        return new DigitalExchangeComponentListProcessor(request, joinedList);
    }

    private static RestListRequest filterByType(final RestListRequest requestList, final String componentType) {
        final Filter filter = new Filter();
        filter.setAttribute("type");
        filter.setValue(componentType);
        filter.setOperator(FilterOperator.EQUAL.getValue());
        requestList.addFilter(filter);
        return requestList;
    }
}
