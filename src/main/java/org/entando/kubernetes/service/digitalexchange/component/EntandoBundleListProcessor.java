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
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterUtils;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;

public class EntandoBundleListProcessor extends RequestListProcessor<EntandoBundle> {

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String TYPE = "type";
    private static final String VERSION = "version";
    private static final String DESCRIPTION = "description";
    private static final String INSTALLED = "installed";

    public EntandoBundleListProcessor(PagedListRequest listRequest, List<EntandoBundle> components) {
        super(listRequest, components);
    }

    public EntandoBundleListProcessor(PagedListRequest listRequest, Stream<EntandoBundle> components) {
        super(listRequest, components);
    }

    @Override
    protected Function<Filter, Predicate<EntandoBundle>> getPredicates() {
        return filter -> {
            switch (filter.getAttribute()) {
                case ID:
                case CODE:
                    return c -> FilterUtils.filterString(filter, c.getCode());
                case NAME:
                case TITLE:
                    return c -> FilterUtils.filterString(filter, c.getTitle());
                case TYPE:
                    return c -> c.getComponentTypes().stream().anyMatch(t -> FilterUtils.filterString(filter, t));
                case DESCRIPTION:
                    return c -> FilterUtils.filterString(filter, c.getDescription());
                case INSTALLED:
                    return c -> FilterUtils.filterBoolean(filter, c.isInstalled());
                case VERSION:
                    return c -> c.getVersions().stream()
                            .anyMatch(cv -> FilterUtils.filterString(filter, cv.getVersion()));
                default:
                    return null;
            }
        };
    }

    @Override
    protected Function<String, Comparator<EntandoBundle>> getComparators() {
        return sort -> {
            switch (sort) {
                case NAME:
                case TITLE:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getTitle(), b.getTitle());
                case ID: //default comparator field
                case CODE:
                default:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getCode(), b.getCode());
            }
        };
    }
}
