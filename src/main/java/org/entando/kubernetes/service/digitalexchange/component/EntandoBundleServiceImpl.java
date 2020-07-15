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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.controller.digitalexchange.component.EntandoBundle;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EntandoBundleServiceImpl implements EntandoBundleService {

    private final K8SServiceClient k8SServiceClient;
    private final List<String> accessibleDigitalExchanges;
    private final EntandoBundleJobRepository jobRepository;
    private final InstalledEntandoBundleRepository installedComponentRepo;
    private final EntandoBundleComponentJobRepository jobComponentRepository;

    public EntandoBundleServiceImpl(K8SServiceClient k8SServiceClient,
            @Value("${entando.digital-exchanges.name:}") List<String> accessibleDigitalExchanges,
            EntandoBundleJobRepository jobRepository,
            EntandoBundleComponentJobRepository jobComponentRepository,
            InstalledEntandoBundleRepository installedComponentRepo) {
        this.k8SServiceClient = k8SServiceClient;
        this.accessibleDigitalExchanges = accessibleDigitalExchanges
                .stream().filter(Strings::isNotBlank).collect(Collectors.toList());
        this.jobRepository = jobRepository;
        this.jobComponentRepository = jobComponentRepository;
        this.installedComponentRepo = installedComponentRepo;
    }

    @Override
    public PagedMetadata<EntandoBundle> getComponents() {
        return getComponents(new PagedListRequest());
    }

    @Override
    public PagedMetadata<EntandoBundle> getComponents(PagedListRequest request) {
        List<EntandoBundleEntity> allComponents = getAllComponents();
        List<EntandoBundleEntity> localFilteredList = new EntandoBundleListProcessor(request, allComponents)
                .filterAndSort().toList();

        List<EntandoBundle> sublist = request.getSublist(localFilteredList) //Get page
                .stream().map(EntandoBundle::fromEntity) //Convert to DTO
                .collect(Collectors.toList());

        return new PagedMetadata<>(request, sublist, localFilteredList.size());
    }

    private List<EntandoBundleEntity> getAllComponents() {
        List<EntandoBundleEntity> allComponents = new ArrayList<>();
        List<EntandoBundleEntity> installedComponents = installedComponentRepo.findAll();
        List<EntandoBundleEntity> externalComponents = getAvailableComponentsFromDigitalExchanges();
        List<EntandoBundleEntity> notAlreadyInstalled = installedComponents.isEmpty() ?
                externalComponents :
                filterNotInstalledComponents(externalComponents, installedComponents);

        allComponents.addAll(installedComponents);
        allComponents.addAll(notAlreadyInstalled);
        return allComponents;
    }

    @Override
    public Optional<EntandoBundle> getInstalledComponent(String id) {
        return installedComponentRepo.findById(id)
                .map(EntandoBundle::fromEntity);
    }

    @Override
    public List<EntandoBundleComponentJob> getBundleInstalledComponents(String id) {
        EntandoBundle bundle = getInstalledComponent(id)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + id + " is not installed in the system"));
        if (bundle.getJob() != null && bundle.getJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            return jobComponentRepository.findAllByParentJob(bundle.getJob());
        } else {
            throw new EntandoComponentManagerException("Bundle " + id + " is not installed correctly");
        }
    }


    private List<EntandoBundleEntity> filterNotInstalledComponents(
            List<EntandoBundleEntity> externalComponents, List<EntandoBundleEntity> installedComponents) {
        Map<String, String> installedVersions = installedComponents.stream()
                .collect(Collectors.toMap(EntandoBundleEntity::getId, EntandoBundleEntity::getVersion));

        List<EntandoBundleEntity> notInstalledComponents = new ArrayList<>();
        for (EntandoBundleEntity dec : externalComponents) {
            String k = dec.getId();
            String v = dec.getVersion();
            if (installedVersions.containsKey(k) && installedVersions.get(k).equals(v)) {
                continue;
            }
            notInstalledComponents.add(dec);
        }

        return notInstalledComponents;
    }

    private List<EntandoBundleEntity> getAvailableComponentsFromDigitalExchanges() {
        List<EntandoComponentBundle> bundles;
        if (accessibleDigitalExchanges.isEmpty()) {
            bundles = k8SServiceClient.getBundlesInObservedNamespaces();
        } else {
            bundles = k8SServiceClient.getBundlesInNamespaces(accessibleDigitalExchanges);
        }
        return bundles.stream().map(this::convertBundleToLegacyComponent).collect(Collectors.toList());
    }


    public EntandoBundleEntity convertBundleToLegacyComponent(EntandoComponentBundle bundle) {
        EntandoBundleEntity dec = EntandoBundleEntity.newFrom(bundle);
        if (checkIfInstalled(bundle)) {
            dec.setInstalled(true);
        }
        return dec;
    }

    private boolean checkIfInstalled(EntandoComponentBundle bundle) {
        String componentId = bundle.getMetadata().getName();
        return jobRepository.findFirstByComponentIdOrderByStartedAtDesc(componentId)
                .map(j -> j.getStatus().equals(JobStatus.INSTALL_COMPLETED))
                .orElse(false);
    }

}
