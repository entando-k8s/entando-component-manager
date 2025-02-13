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

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.web.SystemConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
public class VirtualContextHelper {

    public static final String ENABLED_VIRTUAL_CONTEXTS = System.getenv(SystemConstants.ENTANDO_VIRTUAL_CONTEXTS);

    /**
     * Returns a customized wrapper of the provided request
     */
    public static HttpServletRequest customizeRequest(final HttpServletRequest originalRequest) {
        HttpServletRequest customizedRequest = applyVirtualContext(originalRequest);
        debugRequest(originalRequest, customizedRequest);
        return customizedRequest;
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
            return request;
        }

        return new CustomWrappedRequest(request, requestVirtualContext, new TreeMap<>());
    }

    public static List<String> getVirtualContexts() {
        String virtualContextsAsString = ENABLED_VIRTUAL_CONTEXTS;
        if (virtualContextsAsString != null) {
            return Arrays.asList(virtualContextsAsString.split(SystemConstants.SEPARATOR_CONTEXTS, -1));
        }
        return Collections.emptyList();
    }

    private static void debugRequest(HttpServletRequest originalRequest, HttpServletRequest customizedRequest) {
        log.trace("Original ContextPath: {}", originalRequest.getContextPath());
        log.trace("Original ServletPath: {}", originalRequest.getServletPath());
        log.trace("Customized ContextPath: {}", customizedRequest.getContextPath());
        log.trace("Customized ServletPath: {}", customizedRequest.getServletPath());
    }

    public static String contextPathToContext(String contextPath) {
        if (contextPath == null) return null;
        return contextPath.replaceAll("/$", "").replaceAll("^/", "");
    }
}
