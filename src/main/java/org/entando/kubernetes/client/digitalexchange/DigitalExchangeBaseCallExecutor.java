/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_AUTH;
import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_HTTP_ERROR;
import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_INVALID_URL;
import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_TIMEOUT;
import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_UNREACHABLE;

@Slf4j
public abstract class DigitalExchangeBaseCallExecutor<C extends DigitalExchangeBaseCall<R>, R> {

    private final MessageSource messageSource;
    private final DigitalExchange digitalExchange;
    private final RestTemplate restTemplate;
    private final C call;

    protected DigitalExchangeBaseCallExecutor(MessageSource messageSource,
                                              DigitalExchange digitalExchange, RestTemplate restTemplate,
                                              C call) {

        this.messageSource = messageSource;
        this.digitalExchange = digitalExchange;
        this.restTemplate = restTemplate;
        this.call = call;
    }

    protected R getResponse() {

        String url;

        try {
            url = call.getURL(digitalExchange);
        } catch (IllegalArgumentException ex) {
            log.error("Error calling {}. Invalid URL", digitalExchange.getUrl());
            return getErrorResponse(ERRCODE_DE_INVALID_URL, "digitalExchange.invalidUrl", digitalExchange.getName(), digitalExchange.getUrl());
        }

        try {
            return executeCall(restTemplate, url, call);

        } catch (RestClientResponseException ex) { // Error response

            return call.handleErrorResponse(ex).orElseGet(() -> {
                log.error("Error calling {}. Status code: {}", url, ex.getRawStatusCode());
                return getErrorResponse(ERRCODE_DE_HTTP_ERROR, "digitalExchange.httpError", digitalExchange.getName(), ex.getRawStatusCode());
            });

        } catch (ResourceAccessException ex) { // Other (e.g. unknown host)

            return manageResourceAccessException(url, ex);

        } catch (OAuth2Exception ex) {

            if (ex.getCause() instanceof ResourceAccessException) { // Server down, unknown host, ...
                return manageResourceAccessException(url, (ResourceAccessException) ex.getCause());
            } else {
                log.error("Error calling {}. Exception message: {}", url, ex.getMessage());
                return getErrorResponse(ERRCODE_DE_AUTH, "digitalExchange.oauth2Error", digitalExchange.getName());
            }

        } catch (Throwable t) {
            log.error("Error calling {}", url);
            log.error("Unexpected exception", t);
            throw t;
        }
    }

    protected abstract R executeCall(RestTemplate restTemplate, String url, C call);

    private R manageResourceAccessException(String url, ResourceAccessException ex) {

        if (ex.getCause() instanceof SocketTimeoutException) {
            log.error("Timeout calling {}", url);

            return getErrorResponse(ERRCODE_DE_TIMEOUT, "digitalExchange.timeout",
                    digitalExchange.getName(), digitalExchange.getTimeout());
        }

        log.error("Error calling {}. Exception message: {}", url, ex.getMessage());
        return getErrorResponse(ERRCODE_DE_UNREACHABLE, "digitalExchange.unreachable", digitalExchange.getName());
    }

    protected R getErrorResponse(String errorCode, String msgCode, Object... msgParams) {
        String errorMessage = messageSource.getMessage(msgCode, msgParams, null);
        return buildErrorResponse(errorCode, errorMessage);
    }

    protected abstract R buildErrorResponse(String errorCode, String errorMessage);

    protected DigitalExchange getDigitalExchange() {
        return digitalExchange;
    }

    protected C getCall() {
        return call;
    }
}
