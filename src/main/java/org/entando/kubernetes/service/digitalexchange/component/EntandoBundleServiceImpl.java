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
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.ModelConverter;
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
    public PagedMetadata<EntandoBundle> listBundles() {
        return listBundles(new PagedListRequest());
    }

    @Override
    public PagedMetadata<EntandoBundle> listBundles(PagedListRequest request) {
        List<EntandoBundle> allComponents = getAllComponents();
        List<EntandoBundle> localFilteredList = new EntandoBundleListProcessor(request, allComponents)
                .filterAndSort().toList();

        List<EntandoBundle> sublist = request.getSublist(localFilteredList);

        return new PagedMetadata<>(request, sublist, localFilteredList.size());
    }

    private List<EntandoBundle> getAllComponents() {
        return mergeInstalledBundlesWithEcrBundles(installedComponentRepo.findAll(), getComponentsFromECR());
    }

    private List<EntandoBundle> mergeInstalledBundlesWithEcrBundles(List<EntandoBundleEntity> installedBundles,
            List<EntandoComponentBundle> externalComponents) {

        Map<String, EntandoBundle> bundleMap = installedBundles.stream()
                .map(ModelConverter::fromEntity)
                .collect(Collectors.toMap(EntandoBundle::getEcrId, eb -> eb));

        List<EntandoBundle> ecrBundleList = externalComponents.stream().map(ModelConverter::fromECR).collect(Collectors.toList());

        for (EntandoBundle ecrBundle : ecrBundleList) {
            bundleMap.merge(ecrBundle.getEcrId(), ecrBundle, (installedBundle, externalBundle) -> {
                installedBundle.setVersions(ecrBundle.getVersions());
                return installedBundle;
            });
        }

        return new ArrayList<>(bundleMap.values());

    }

    @Override
    public Optional<EntandoBundle> getInstalledBundle(String ecrId) {
        return installedComponentRepo.findByEcrId(ecrId)
                .map(ModelConverter::fromEntity);
    }

    @Override
    public List<EntandoBundleComponentJob> getInstalledBundleComponents(String ecrId) {
        EntandoBundle bundle = getInstalledBundle(ecrId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + ecrId + " is not installed in the system"));
        if (bundle.getLastJob() != null && bundle.getLastJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            return jobComponentRepository.findAllByParentJobId(bundle.getLastJob().getId());
        } else {
            throw new EntandoComponentManagerException("Bundle " + ecrId + " is not installed correctly");
        }
    }

    private List<EntandoComponentBundle> getComponentsFromECR() {
        List<EntandoComponentBundle> bundles;
        if (accessibleDigitalExchanges.isEmpty()) {
            bundles = k8SServiceClient.getBundlesInObservedNamespaces();
        } else {
            bundles = k8SServiceClient.getBundlesInNamespaces(accessibleDigitalExchanges);
        }
        return bundles;
    }

}
