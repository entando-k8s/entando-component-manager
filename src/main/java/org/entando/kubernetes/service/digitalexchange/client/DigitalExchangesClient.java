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
package org.entando.kubernetes.service.digitalexchange.client;

import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.web.response.RestResponse;

import java.io.InputStream;
import java.util.List;

public interface DigitalExchangesClient {

    /**
     * Calls in parallel all the configured and active DE instances and returns
     * a combined result of all the responses.
     *
     * @param <R> the type of each DE response
     * @param <C> the type of the combined response
     * @param digitalExchanges the list of digital exchanges to combine the result
     * @param call information necessary to execute the HTTP call and process
     * the result
     * @return a combined response
     */
    <R extends RestResponse<?, ?>, C> C getCombinedResult(List<DigitalExchange> digitalExchanges, DigitalExchangeCall<R, C> call);

    <R extends RestResponse<?, ?>, C> R getSingleResponse(DigitalExchange digitalExchange, DigitalExchangeCall<R, C> call);
    
    InputStream getStreamResponse(DigitalExchange digitalExchange, DigitalExchangeBaseCall<InputStream> call);
}
