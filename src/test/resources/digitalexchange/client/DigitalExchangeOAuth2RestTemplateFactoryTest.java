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

package org.entando.kubernetes.digitalexchange.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.kubernetes.client.digitalexchange.DigitalExchangeRestTemplateFactory;
import org.entando.kubernetes.client.digitalexchange.DigitalExchangeRestTemplateFactoryImpl;
import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.junit.Before;
import org.junit.Test;

public class DigitalExchangeOAuth2RestTemplateFactoryTest {

    private DigitalExchangeRestTemplateFactory restTemplateFactory;

    @Before
    public void setUp() {
        restTemplateFactory = new DigitalExchangeRestTemplateFactoryImpl();
    }

    @Test
    public void shouldCreateTemplate() {
        DigitalExchange de = new DigitalExchange();
        de.setUrl("http://www.entando.com");
        de.setTimeout(5000);

        assertThat(restTemplateFactory.createRestTemplate(de)).isNotNull();
    }

    @Test
    public void shouldCreateTemplateWithDefaultTimeout() {
        DigitalExchange de = new DigitalExchange();
        de.setUrl("http://www.entando.com");

        assertThat(restTemplateFactory.createRestTemplate(de)).isNotNull();
    }

    @Test
    public void shouldReturnNullForInvalidURL() {
        DigitalExchange de = new DigitalExchange();
        de.setUrl("invalid-url");

        assertThat(restTemplateFactory.createRestTemplate(de)).isNull();
    }
}
