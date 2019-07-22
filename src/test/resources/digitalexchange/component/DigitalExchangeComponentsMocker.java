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
package org.entando.kubernetes.digitalexchange.component;

import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.digitalexchange.client.DigitalExchangeMockedRequest;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentListProcessor;
import org.entando.web.request.RestListRequest;
import org.entando.web.response.PagedMetadata;
import org.entando.web.response.PagedRestResponse;
import org.entando.web.response.RestResponse;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DigitalExchangeComponentsMocker {

    public static Function<DigitalExchangeMockedRequest, RestResponse<?, ?>> mock(String... ids) {
        return request -> mock(request, getComponentsStream(ids));
    }

    public static List<DigitalExchangeComponent> getMockedComponents(String... ids) {
        return getComponentsStream(ids).collect(Collectors.toList());
    }

    private static Stream<DigitalExchangeComponent> getComponentsStream(String... ids) {
        return Arrays.stream(ids).map(id
                -> new DigitalExchangeComponentBuilder(id)
                        .setLastUpdate(new Date())
                        .setVersion("5.1.0")
                        .setType("pageModel")
                        .setRating(5)
                        .build());
    }

    public static Function<DigitalExchangeMockedRequest, RestResponse<?, ?>> mock(DigitalExchangeComponent... components) {
        return request -> mock(request, Arrays.stream(components));
    }

    private static PagedRestResponse<DigitalExchangeComponent> mock(DigitalExchangeMockedRequest request, Stream<DigitalExchangeComponent> stream) {

        stream = new DigitalExchangeComponentListProcessor(request.getRestListRequest(), stream)
                .filterAndSort().getStream();

        List<DigitalExchangeComponent> components = stream.collect(Collectors.toList());
        PagedMetadata<DigitalExchangeComponent> pagedMetadata
                = new PagedMetadata<>(new RestListRequest(), components, components.size());

        return new PagedRestResponse<>(pagedMetadata);
    }
}
