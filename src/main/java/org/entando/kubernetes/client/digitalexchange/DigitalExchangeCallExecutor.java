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

import static org.entando.kubernetes.client.digitalexchange.DigitalExchangesClientImpl.ERRCODE_DE_WRONG_PAYLOAD;

import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.entando.web.response.RestError;
import org.entando.web.response.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class DigitalExchangeCallExecutor<R extends RestResponse<?, ?>, C> extends
        DigitalExchangeBaseCallExecutor<DigitalExchangeCall<R, C>, R> {

    private static final Logger logger = LoggerFactory.getLogger(DigitalExchangeCallExecutor.class);

    protected DigitalExchangeCallExecutor(MessageSource messageSource,
            DigitalExchange digitalExchange, RestTemplate restTemplate,
            DigitalExchangeCall<R, C> call) {

        super(messageSource, digitalExchange, restTemplate, call);
    }

    @Override
    protected R executeCall(RestTemplate restTemplate, String url, DigitalExchangeCall<R, C> call) {
        final ResponseEntity<R> responseEntity = restTemplate
                .exchange(url, call.getMethod(), call.getEntity(), call.getParameterizedTypeReference());
        final R response = responseEntity.getBody();

        if (call.isResponseParsable(response)) {
            return response;
        } else {
            logger.error("Error calling {}. Unable to parse response", url);
            return getErrorResponse(ERRCODE_DE_WRONG_PAYLOAD, "digitalExchange.unparsableResponse", getDigitalExchange().getName());
        }
    }

    @Override
    protected R buildErrorResponse(String errorCode, String errorMessage) {
        R errorResponse = getCall().getEmptyRestResponse();
        errorResponse.addError(new RestError(errorCode, errorMessage));
        return errorResponse;
    }
}
