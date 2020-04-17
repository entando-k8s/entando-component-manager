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

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterUtils;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;

public class DigitalExchangeComponentListProcessor extends RequestListProcessor<DigitalExchangeComponent> {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String INSTALLED = "installed";
    private static final String VERSION = "version";
    private static final String DESCRIPTION = "description";

    public DigitalExchangeComponentListProcessor(PagedListRequest listRequest, List<DigitalExchangeComponent> components) {
        super(listRequest, components);
    }

    public DigitalExchangeComponentListProcessor(PagedListRequest listRequest, Stream<DigitalExchangeComponent> components) {
        super(listRequest, components);
    }

    @Override
    protected Function<Filter, Predicate<DigitalExchangeComponent>> getPredicates() {
        return filter -> {
            switch (filter.getAttribute()) {
                case ID:
                    return c -> FilterUtils.filterString(filter, c.getId());
                case NAME:
                    return c -> FilterUtils.filterString(filter, c.getName());
                case TYPE:
                    return c -> FilterUtils.filterString(filter, c.getType());
                case VERSION:
                    return c -> FilterUtils.filterString(filter, c.getVersion());
                case DESCRIPTION:
                    return c -> FilterUtils.filterString(filter, c.getDescription());
                case INSTALLED:
                    return c -> FilterUtils.filterBoolean(filter, c.isInstalled());
                default:
                    return null;
            }
        };
    }

    @Override
    protected Function<String, Comparator<DigitalExchangeComponent>> getComparators() {
        return sort -> {
            switch (sort) {
                case VERSION:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getVersion(), b.getVersion());
                case TYPE:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getType(), b.getType());
                case INSTALLED:
                    return (a, b) -> Boolean.compare(a.isInstalled(), b.isInstalled());
                case NAME:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getName(), b.getName());
                case ID: // id is the default sorting field
                default:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getId(), b.getId());
            }
        };
    }
}
