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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DigitalExchangeComponentsServiceImpl implements DigitalExchangeComponentsService {

    private static final List<String> LOCAL_FILTERS = Arrays.asList("digitalExchangeName", "digitalExchangeId", "installed");

//    private final @NonNull DigitalExchangesClient client;
//    private final @NonNull DigitalExchangesService exchangesService;
//    private final @NonNull
//    KubernetesService kubernetesService;
    private final @NonNull K8SServiceClient k8SServiceClient;

//    @Override public ResilientPagedMetadata<DigitalExchangeComponent> getComponents(final PagedListRequest requestList) {
//        final ResilientPagedMetadata<DigitalExchangeComponent> combinedResult = client.getCombinedResult(
//                exchangesService.getDigitalExchanges(),
//                new ComponentsCall(exchangesService, buildForwardedRequest(requestList)));
//        final List<DigitalExchangeComponent> localFilteredList = new DigitalExchangeComponentListProcessor(
//                requestList, combinedResult.getBody()).filterAndSort().toList();
//        final List<DigitalExchangeComponent> sublist = requestList.getSublist(localFilteredList);
//        sublist.forEach(this::processInstalled);
//
//        combinedResult.setTotalItems(localFilteredList.size());
//        combinedResult.setBody(sublist);
//        combinedResult.setPage(requestList.getPage());
//        combinedResult.setPageSize(requestList.getPageSize());
//
//        return combinedResult;
//    }

//    @Override
//    public SimpleRestResponse<DigitalExchangeComponent> getComponent(final DigitalExchange digitalExchange, final String componentId) {
//        final SimpleDigitalExchangeCall<DigitalExchangeComponent> call = new SimpleDigitalExchangeCall<>(
//                HttpMethod.GET, new ParameterizedTypeReference<SimpleRestResponse<DigitalExchangeComponent>>() {
//        }, "digitalExchange", "components", componentId);
//        final SimpleRestResponse<DigitalExchangeComponent> response = client.getSingleResponse(digitalExchange, call);
//        processInstalled(response.getPayload());
//        return response;
//    }

    @Override
    public List<DigitalExchangeComponent> getComponents() {
         List<EntandoDeBundle> bundles = k8SServiceClient.getBundlesInNamespace(DEFAULT_BUNDLE_NAMESPACE);
         return bundles.stream().map(this::convertBundleToLegacyComponent).collect(Collectors.toList());
    }


    public DigitalExchangeComponent convertBundleToLegacyComponent(EntandoDeBundle bundle) {
        DigitalExchangeComponent dec = new DigitalExchangeComponent();
        EntandoDeBundleDetails bd = bundle.getSpec().getDetails();
        dec.setDescription(bd.getDescription());
        dec.setDigitalExchangeId(DEFAULT_BUNDLE_NAMESPACE);
        dec.setDigitalExchangeName("Default-digital-exchange");
        dec.setId(bd.getName());
        dec.setRating(5);
        dec.setInstalled(false);
        dec.setType("Bundle");
        dec.setLastUpdate(new Date());
        dec.setSignature("");
        dec.setVersion(bd.getDistTags().get("latest").toString());
        return dec;
    }

//    private void processInstalled(final DigitalExchangeComponent component) {
//        if (kubernetesService.isLinkedPlugin(component.getId())) {
//            component.setInstalled(true);
//        }
//    }

//    private PagedListRequest buildForwardedRequest(final PagedListRequest originalRequest) {
//        final PagedListRequest forwaredRequest = new PagedListRequest();
//        forwaredRequest.setDirection(originalRequest.getDirection());
//        forwaredRequest.setSort(originalRequest.getSort());
//        forwaredRequest.setPageSize(Integer.MAX_VALUE);
//        forwaredRequest.setPage(1);
//
//        if (originalRequest.getFilters() != null) {
//            final Filter[] forwaredFilters = Arrays.stream(originalRequest.getFilters())
//                    .filter(f -> !LOCAL_FILTERS.contains(f.getAttribute()))
//                    .toArray(Filter[]::new);
//            forwaredRequest.setFilters(forwaredFilters);
//        }
//
//        return forwaredRequest;
//    }
}
