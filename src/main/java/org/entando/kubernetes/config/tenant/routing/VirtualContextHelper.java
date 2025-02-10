/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.kubernetes.config.tenant.routing;

import org.entando.kubernetes.model.web.SystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class VirtualContextHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualContextHelper.class);
    public static final String ENABLED_VIRTUAL_CONTEXTS = System.getenv(SystemConstants.ENTANDO_VIRTUAL_CONTEXTS);


    /**
     * Returns a customized wrapper of the provided request
     */
    public static HttpServletRequest customizeRequest(final HttpServletRequest originalRequest) {
        debugRequest("[ORIG]", originalRequest);
        HttpServletRequest customizedRequest = applyVirtualContext(originalRequest);
        debugRequest("[CUST]", customizedRequest);
        return customizedRequest;
    }

    private static void debugRequest(String tag, HttpServletRequest customRequest) {
        LOGGER.info(" * FILTER: {} ContextPath: {}", tag, customRequest.getContextPath());
        LOGGER.info(" * FILTER: {} ServletPath: {}", tag, customRequest.getServletPath());
    }

    /**
     * This method modifies on the fly, by wrapping it, the REQUEST
     *
     * @param request the original request
     * @return the wrapped request if modification criteria are met, the original request otherwise
     */
    private static HttpServletRequest applyVirtualContext(HttpServletRequest request) {
        List<String> allowedVirtualContexts = getVirtualContexts();

        if (allowedVirtualContexts.isEmpty()) {
            return request;
        }

        String[] parts = request.getServletPath().split("/");
        String requestVirtualContext = (parts.length >= 2) ? parts[1] : null;

        if (requestVirtualContext == null || !allowedVirtualContexts.contains(requestVirtualContext)) {
            LOGGER.error(invalidVirtualContext(requestVirtualContext).getMessage());
            return request;
        }

        return new CustomWrappedRequest(request, requestVirtualContext, new TreeMap<>());
    }

    private static HttpClientErrorException invalidVirtualContext(String requestVirtualContext) {
        return (requestVirtualContext != null)
                ? new HttpClientErrorException(HttpStatus.NOT_FOUND, String.format("The requested virtual context \"%s\" doesn't exist", requestVirtualContext))
                : new HttpClientErrorException(HttpStatus.NOT_FOUND, "The requested null virtual context doesn't exist");
    }

    public static List<String> getVirtualContexts() {
        String virtualContextsAsString = ENABLED_VIRTUAL_CONTEXTS;
        if (virtualContextsAsString != null) {
            return Arrays.asList(virtualContextsAsString.split(SystemConstants.SEPARATOR_CONTEXTS, -1));
        }
        return Collections.emptyList();
    }

}
