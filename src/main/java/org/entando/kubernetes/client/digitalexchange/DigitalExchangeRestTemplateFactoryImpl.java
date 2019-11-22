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

import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DigitalExchangeRestTemplateFactoryImpl implements DigitalExchangeRestTemplateFactory {

    private static final Logger logger = LoggerFactory.getLogger(DigitalExchangeRestTemplateFactoryImpl.class);

    private static final int DEFAULT_TIMEOUT = 10000;

    @Value("${entando.auth-url}")
    private String ENTANDO_AUTH_URL;

    @Override
    public RestTemplate createRestTemplate(final DigitalExchange digitalExchange) {
        if (digitalExchange.getClientId() == null || digitalExchange.getClientSecret() == null) {
            final RestTemplate template = new RestTemplate();
            template.setRequestFactory(getRequestFactory(digitalExchange));
            return template;
        } else {
            final OAuth2ProtectedResourceDetails resourceDetails = getResourceDetails(digitalExchange);
            if (resourceDetails == null) {
                return null;
            }
            final OAuth2RestTemplate template = new OAuth2RestTemplate(resourceDetails);
            template.setRequestFactory(getRequestFactory(digitalExchange));
            template.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());
            return template;
        }
    }

    private OAuth2ProtectedResourceDetails getResourceDetails(final DigitalExchange digitalExchange) {
        final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setAuthenticationScheme(AuthenticationScheme.header);
        resourceDetails.setClientId(digitalExchange.getClientId());
        resourceDetails.setClientSecret(digitalExchange.getClientSecret());
        resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.form);
        try {
            resourceDetails.setAccessTokenUri(getTokenUri(digitalExchange));
        } catch (IllegalArgumentException ex) {
            logger.error("DigitalExchange {} has been configured with a wrong URL: {}",
                    digitalExchange.getName(), digitalExchange.getUrl());
            return null;
        }
        return resourceDetails;
    }

    private String getTokenUri(final DigitalExchange digitalExchange) {
        return UriComponentsBuilder.fromUriString(ENTANDO_AUTH_URL).toUriString();
    }

    private ClientHttpRequestFactory getRequestFactory(final DigitalExchange digitalExchange) {
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        final int timeout = digitalExchange.getTimeout() > 0 ? digitalExchange.getTimeout() : DEFAULT_TIMEOUT;

        requestFactory.setConnectionRequestTimeout(timeout);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }
}
