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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DigitalExchangeComponentsServiceImpl implements DigitalExchangeComponentsService {

    private final K8SServiceClient k8SServiceClient;
    private final List<String> accessibleDigitalExchanges;
    private final DigitalExchangeJobRepository jobRepository;
    private final DigitalExchangeInstalledComponentRepository installedComponentRepo;

    public DigitalExchangeComponentsServiceImpl(K8SServiceClient k8SServiceClient,
            @Value("${entando.digital-exchanges.name:}") List<String> accessibleDigitalExchanges,
            DigitalExchangeJobRepository jobRepository,
            DigitalExchangeInstalledComponentRepository installedComponentRepo) {
        this.k8SServiceClient = k8SServiceClient;
        this.accessibleDigitalExchanges = accessibleDigitalExchanges
                .stream().filter(Strings::isNotBlank).collect(Collectors.toList());
        this.jobRepository = jobRepository;
        this.installedComponentRepo = installedComponentRepo;
    }


    @Override
    public PagedMetadata<DigitalExchangeComponent> getComponents() {
        return getComponents(new PagedListRequest());
    }

    @Override
    public PagedMetadata<DigitalExchangeComponent> getComponents(PagedListRequest request) {
        List<DigitalExchangeComponent> allComponents = new ArrayList<>();
        List<DigitalExchangeComponent> installedComponents = installedComponentRepo.findAll();
        List<DigitalExchangeComponent> externalComponents = getAvailableComponentsFromDigitalExchanges();
        List<DigitalExchangeComponent> notAlreadyInstalled = installedComponents.isEmpty() ?
                externalComponents :
                filterNotInstalledComponents(externalComponents, installedComponents);

        allComponents.addAll(installedComponents);
        allComponents.addAll(notAlreadyInstalled);
        List<DigitalExchangeComponent>  localFilteredList = new DigitalExchangeComponentListProcessor(request, allComponents)
                .filterAndSort().toList();
        List<DigitalExchangeComponent> sublist = request.getSublist(localFilteredList);

        PagedMetadata<DigitalExchangeComponent> result = new PagedMetadata<>();
        result.setBody(sublist);
        result.setTotalItems(localFilteredList.size());
        result.setPage(request.getPage());
        result.setPageSize(request.getPageSize());
        return result;
    }

    private List<DigitalExchangeComponent> filterNotInstalledComponents(
            List<DigitalExchangeComponent> externalComponents, List<DigitalExchangeComponent> installedComponents) {
        Map<String, String> installedVersions = installedComponents.stream()
                .collect(Collectors.toMap(DigitalExchangeComponent::getId, DigitalExchangeComponent::getVersion));

        List<DigitalExchangeComponent> notInstalledComponents = new ArrayList<>();
        for (DigitalExchangeComponent dec: externalComponents) {
            String k = dec.getId();
            String v = dec.getVersion();
            if (installedVersions.containsKey(k) && installedVersions.get(k).equals(v)) {
                continue;
            }
            notInstalledComponents.add(dec);
        }

        return notInstalledComponents;
    }

    private List<DigitalExchangeComponent> getAvailableComponentsFromDigitalExchanges() {
        List<EntandoDeBundle> bundles;
        if(accessibleDigitalExchanges.isEmpty()) {
            bundles = k8SServiceClient.getBundlesInObservedNamespaces();
        } else {
            bundles = k8SServiceClient.getBundlesInNamespaces(accessibleDigitalExchanges);
        }
        return bundles.stream().map(this::convertBundleToLegacyComponent).collect(Collectors.toList());
    }


    public DigitalExchangeComponent convertBundleToLegacyComponent(EntandoDeBundle bundle) {
        DigitalExchangeComponent dec = DigitalExchangeComponent.newFrom(bundle);
        dec.setInstalled(checkIfInstalled(bundle));
        return dec;
    }

    private boolean checkIfInstalled(EntandoDeBundle bundle) {
        String deId = bundle.getMetadata().getNamespace();
        String componentId = bundle.getMetadata().getName();
        return jobRepository.findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(deId, componentId)
                .map(j -> j.getStatus().equals(JobStatus.INSTALL_COMPLETED))
                .orElse(false);
    }

}
