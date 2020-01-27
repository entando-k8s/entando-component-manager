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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.entando.kubernetes.model.web.ResilientPagedMetadata;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.RestError;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

/**
 * Provides the logic for combining a set of PagedRestResponse retrieved from DE
 * instances.
 */
public abstract class PagedDigitalExchangeCall<T> extends DigitalExchangeCall<PagedRestResponse<T>, ResilientPagedMetadata<T>> {

    private final PagedListRequest request;

    public PagedDigitalExchangeCall(final HttpMethod method,
                                    final PagedListRequest listRequest,
                                    final ParameterizedTypeReference<PagedRestResponse<T>> parameterizedTypeReference,
                                    final String ... urlSegments) {
        super(method, parameterizedTypeReference, urlSegments);
        this.request = listRequest;
    }

    public PagedDigitalExchangeCall(final PagedListRequest listRequest,
                                    final ParameterizedTypeReference<PagedRestResponse<T>> parameterizedTypeReference,
                                    final String ... urlSegments) {
        this(HttpMethod.GET, listRequest, parameterizedTypeReference, urlSegments);
    }

    @Override
    protected boolean isResponseParsable(final PagedRestResponse<T> response) {
        return super.isResponseParsable(response) && response.getPayload() != null;
    }

    @Override
    protected String getURL(final DigitalExchange digitalExchange) {
        String url = super.getURL(digitalExchange);
        return new PagedListRequestUriBuilder(url, transformRequest(request)).toUriString();
    }

    /**
     * When we forward a PagedListRequest to a DE instance we need to modify the
     * pagination parameters, because filtering, sorting and pagination will be
     * applied to the combined result and we need to retrieve all the
     * potentially useful items.<br/>
     * Example: if the user asks for page number 2 with a page size of 5 we need
     * to retrieve the first 10 results from each DE instance in order to be
     * sure to obtain the correct combined result.
     */
    private PagedListRequest transformRequest(PagedListRequest userRequest) {
        PagedListRequest digitalExchangeRequest = new PagedListRequest();

        // retrieve all the items in one page
        digitalExchangeRequest.setPage(1);
        digitalExchangeRequest.setPageSize(userRequest.getPage() * userRequest.getPageSize());

        // copy the other fields
        digitalExchangeRequest.setSort(userRequest.getSort());
        digitalExchangeRequest.setDirection(userRequest.getDirection());
        digitalExchangeRequest.setFilters(userRequest.getFilters());

        return digitalExchangeRequest;
    }

    @Override
    protected PagedRestResponse<T> getEmptyRestResponse() {
        return new PagedRestResponse<>(new PagedMetadata<>());
    }

    /**
     * This method can be overridden in order to manipulate a single response
     * before combining it.
     */
    protected void preprocessResponse(String exchangeName, PagedRestResponse<T> response) {
    }

    @Override
    public ResilientPagedMetadata<T> combineResults(final Map<String, PagedRestResponse<T>> allResults) {
        final List<RestError> errors = new ArrayList<>();
        List<T> joinedList = new ArrayList<>();
        int total = 0;

        for (Map.Entry<String, PagedRestResponse<T>> entry : allResults.entrySet()) {
            final PagedRestResponse<T> response = entry.getValue();

            preprocessResponse(entry.getKey(), response);

            if (response.getErrors().isEmpty()) {
                joinedList.addAll(response.getPayload());
                total += response.getMetadata().getTotalItems();
            } else {
                errors.addAll(response.getErrors());
            }
        }

        final RequestListProcessor<T> requestListProcessor = getRequestListProcessor(request, joinedList);
        if (requestListProcessor != null) {
            joinedList = requestListProcessor.filterAndSort().toList();
        }

        final List<T> subList = request.getSublist(joinedList);
        final ResilientPagedMetadata<T> pagedMetadata = new ResilientPagedMetadata<>(request, subList, total);

        pagedMetadata.setFilters(request.getFilters());
        pagedMetadata.setErrors(errors);

        return pagedMetadata;
    }

    protected abstract RequestListProcessor<T> getRequestListProcessor(PagedListRequest request, List<T> joinedList);
}
