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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoDefaultOAuth2RequestAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class TenantRestTemplateAccessor {
    
    @Autowired(required = false)
    private Map<String, TenantConfig> tenantsConfigs;
    
    private final OAuth2RestTemplate restTemplate;
    
    private Map<String, RestTemplate> tenantsTemplates = new ConcurrentHashMap<>();
    
    public TenantRestTemplateAccessor(
            @Value("${spring.security.oauth2.client.registration.oidc.client-id}") final String clientId,
            @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") final String clientSecret,
            @Value("${entando.auth-url}") final String tokenUri) {
        final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setAuthenticationScheme(AuthenticationScheme.header);
        resourceDetails.setClientId(clientId);
        resourceDetails.setClientSecret(clientSecret);
        resourceDetails.setAccessTokenUri(tokenUri);
        this.restTemplate = new OAuth2RestTemplate(resourceDetails);
        this.restTemplate.setAuthenticator(new EntandoDefaultOAuth2RequestAuthenticator());
        this.restTemplate.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());
    }
    
    public RestTemplate getRestTemplate() {
        String tenantCode = TenantContext.getCurrentTenant();
        RestTemplate currentRestTemplate = (StringUtils.isEmpty(tenantCode)) ? this.restTemplate : tenantsTemplates.computeIfAbsent(tenantCode, this::createRestTemplate);
        currentRestTemplate.getInterceptors().add(new TenantClientHttpRequestInterceptor(tenantCode));
        return currentRestTemplate;
    }
    
    private RestTemplate createRestTemplate(String tenantCode) {
        return tenantsConfigs.values().stream().filter(c -> c.getTenantCode().equals(tenantCode)).findFirst().map(config -> {
            final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
            resourceDetails.setAuthenticationScheme(AuthenticationScheme.header);
            resourceDetails.setClientId(config.getDeKcClientId());
            resourceDetails.setClientSecret(config.getDeKcClientSecret());
            resourceDetails.setAccessTokenUri(config.getKcAuthUrl() + 
                    "/realms/" + config.getKcRealm() + "/protocol/openid-connect/token");
            OAuth2RestTemplate tenantRestTemplate = new OAuth2RestTemplate(resourceDetails);
            tenantRestTemplate.setAuthenticator(new EntandoDefaultOAuth2RequestAuthenticator());
            tenantRestTemplate.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());
            return tenantRestTemplate;
        }).orElse(this.restTemplate);
    }
    
    public static class TenantClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

        private final String tenantCode;

        TenantClientHttpRequestInterceptor(String tenantCode) {
            this.tenantCode = tenantCode;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            if (StringUtils.isNotBlank(this.tenantCode)) {
                request.getHeaders().set("Entando-Tenant-Code", this.tenantCode);
            }
            return execution.execute(request, body);
        }

    }
    
}
