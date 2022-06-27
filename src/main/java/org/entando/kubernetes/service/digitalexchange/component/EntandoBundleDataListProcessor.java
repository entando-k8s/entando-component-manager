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
import org.entando.kubernetes.model.bundle.EntandoBundleData;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterUtils;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;

public class EntandoBundleDataListProcessor extends RequestListProcessor<EntandoBundleData> {

    public static final String ID = "id";
    public static final String BUNDLE_ID = "bundleId";
    public static final String BUNDLE_NAME = "bundleName";
    public static final String COMPONENT_TYPE = "componentType";
    public static final String INSTALLED = "installed";
    public static final String PUBLICATION_URL = "publicationUrl";

    public EntandoBundleDataListProcessor(PagedListRequest listRequest, List<EntandoBundleData> components) {
        super(listRequest, components);
    }

    public EntandoBundleDataListProcessor(PagedListRequest listRequest, Stream<EntandoBundleData> components) {
        super(listRequest, components);
    }

    @Override
    protected Function<Filter, Predicate<EntandoBundleData>> getPredicates() {
        return filter -> {
            switch (filter.getAttribute()) {
                case BUNDLE_ID:
                    return c -> FilterUtils.filterString(filter, c.getBundleId());
                case BUNDLE_NAME:
                    return c -> FilterUtils.filterString(filter, c.getBundleName());
                case COMPONENT_TYPE:
                    return c -> c.getComponentTypes().stream().anyMatch(t -> FilterUtils.filterString(filter, t));
                case INSTALLED:
                    return c -> FilterUtils.filterBoolean(filter, c.isInstalled());
                case PUBLICATION_URL:
                    return c -> FilterUtils.filterString(filter, c.getPublicationUrl());
                // no filter by id because it has no meaning and id is null for bundle CR only
                case ID:
                default:
                    return null;
            }
        };
    }

    @Override
    protected Function<String, Comparator<EntandoBundleData>> getComparators() {
        return sort -> {
            switch (sort) {
                case BUNDLE_ID:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getBundleId(), b.getBundleId());
                case BUNDLE_NAME:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getBundleName(), b.getBundleName());
                case PUBLICATION_URL:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getPublicationUrl(), b.getPublicationUrl());
                case ID:
                default:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getId(), b.getId());
            }
        };
    }
}
