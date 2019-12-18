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
package org.entando.kubernetes.model.digitalexchange;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "digital_exchange")
public class DigitalExchangeEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "timeout")
    private int timeout;

    @Column(name = "active")
    private boolean active;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "public_key")
    private String publicKey;

    @PrePersist
    public void createUuid() {
        id = UUID.randomUUID();
    }

    public DigitalExchangeEntity(final DigitalExchange digitalExchange) {
        apply(digitalExchange);
    }

    public DigitalExchange convert() {
        final DigitalExchange digitalExchange = new DigitalExchange();
        digitalExchange.setId(id.toString());
        digitalExchange.setName(name);
        digitalExchange.setUrl(url);
        digitalExchange.setTimeout(timeout);
        digitalExchange.setActive(active);
        digitalExchange.setClientId(clientId);
        digitalExchange.setClientSecret(clientSecret);
        digitalExchange.setPublicKey(publicKey);
        return digitalExchange;
    }

    public void apply(final DigitalExchange digitalExchange) {
        setName(digitalExchange.getName());
        setUrl(digitalExchange.getUrl());
        setTimeout(digitalExchange.getTimeout());
        setActive(digitalExchange.isActive());
        setClientId(digitalExchange.getClientId());
        setClientSecret(digitalExchange.getClientSecret());
        setPublicKey(digitalExchange.getPublicKey());
    }

}
