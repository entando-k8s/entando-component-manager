/*
 * Copyright 2023-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.kubernetes.config.tenant;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

@Component
@Order(1)
@DependsOn("tenantConfiguration")
class TenantFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    @Autowired(required = false)
    private Map<String, TenantConfig> tenantConfiguration;
    
    @Override
    public void doFilter(ServletRequest request, 
            ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String domain = UrlUtils.fetchServer(req);
        String tenantCode = this.getTenantCodeByDomain(domain);
        logger.error("Tenant Code " + tenantCode);
        if (null != tenantCode) {
            TenantContext.setCurrentTenant(tenantCode);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    
    public String getTenantCodeByDomain(String domain) {
        Set<String> codes = this.tenantConfiguration.keySet();
        String tenantCode =  this.tenantConfiguration.values().stream()
                .filter(v -> v.getFqdns().contains(domain))
                .map(tc -> tc.getTenantCode())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(codes.stream().filter(code -> StringUtils.equals(code, domain)).findFirst().orElse(null));
        if (logger.isDebugEnabled()) {
            logger.debug("From domain:'{}' retrieved tenantCode:'{}' from codes:'{}'",
                    domain, tenantCode, codes.stream().collect(Collectors.joining(",")));
        }
        return tenantCode;
    }
    
}
