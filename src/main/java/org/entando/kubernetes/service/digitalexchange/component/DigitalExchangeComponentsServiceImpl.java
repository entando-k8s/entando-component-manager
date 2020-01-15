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
package org.entando.kubernetes.service.digitalexchange.component;

import static org.entando.kubernetes.client.k8ssvc.K8SServiceClient.DEFAULT_BUNDLE_NAMESPACE;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DigitalExchangeComponentsServiceImpl implements DigitalExchangeComponentsService {

    private static final List<String> LOCAL_FILTERS = Arrays.asList("digitalExchangeName", "digitalExchangeId", "installed");


    private final @NonNull K8SServiceClient k8SServiceClient;


    @Override
    public List<DigitalExchangeComponent> getComponents() {
         List<EntandoDeBundle> bundles = k8SServiceClient.getBundlesInNamespace(DEFAULT_BUNDLE_NAMESPACE);
         return bundles.stream().map(this::convertBundleToLegacyComponent).collect(Collectors.toList());
    }


    public DigitalExchangeComponent convertBundleToLegacyComponent(EntandoDeBundle bundle) {
        DigitalExchangeComponent dec = new DigitalExchangeComponent();
        EntandoDeBundleDetails bd = bundle.getSpec().getDetails();
        dec.setName(bundle.getSpec().getDetails().getName());
        dec.setDescription(bd.getDescription());
        dec.setDigitalExchangeId(DEFAULT_BUNDLE_NAMESPACE);
        dec.setDigitalExchangeName(DEFAULT_BUNDLE_NAMESPACE);
        dec.setId(bd.getName());
        dec.setRating(5);
        dec.setInstalled(false);
        dec.setType("Bundle");
        dec.setLastUpdate(new Date());
        dec.setSignature("");
        dec.setVersion(bd.getDistTags().get("latest").toString());
        dec.setImage("someimage");
        return dec;
    }

}
