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

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.compress.utils.Sets;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleJob;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
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
        this.accessibleDigitalExchanges = Optional.ofNullable(accessibleDigitalExchanges).orElse(new ArrayList<>())
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
        //TODO may generate performance issues if list of bundles is too big
        List<EntandoBundle> allBundles = listAllBundles();
        List<EntandoBundle> localFilteredList = new EntandoBundleListProcessor(request, allBundles)
                .filterAndSort().toList();
        List<EntandoBundle> sublist = request.getSublist(localFilteredList);

        return new PagedMetadata<>(request, sublist, localFilteredList.size());
    }

    private List<EntandoBundle> listAllBundles() {
        List<EntandoBundle> allComponents = new ArrayList<>();
        List<EntandoBundleEntity> installedBundles = installedComponentRepo.findAll();
        List<EntandoBundle> availableBundles = listBundlesFromEcr();
        List<EntandoBundle> installedButNotAvailableOnEcr = filterInstalledButNotAvailableOnEcr(availableBundles, installedBundles);

        allComponents.addAll(availableBundles);
        allComponents.addAll(installedButNotAvailableOnEcr);
        return allComponents;
    }

    @Override
    public Optional<EntandoBundle> getInstalledBundle(String id) {
        return installedComponentRepo.findById(id)
                .map(this::convertToBundleFromEntity);
    }

    @Override
    public List<EntandoBundleComponentJobEntity> getBundleInstalledComponents(String id) {
        EntandoBundle bundle = getInstalledBundle(id)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + id + " is not installed in the system"));
        if (bundle.getInstalledJob() != null && bundle.getInstalledJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            return jobComponentRepository.findAllByParentJobId(bundle.getInstalledJob().getId());
        } else {
            throw new EntandoComponentManagerException("Bundle " + id + " is not installed correctly");
        }
    }

    private List<EntandoBundle> filterInstalledButNotAvailableOnEcr(List<EntandoBundle> availableBundles,
            List<EntandoBundleEntity> installedBundles) {
        //TODO could be a problem if available bundles list is too big
        Set<String> keySet = availableBundles.stream().map(EntandoBundle::getCode).collect(Collectors.toSet());
        return installedBundles.stream()
                .filter(b -> !keySet.contains(b.getId()))
                .map(this::convertToBundleFromEntity)
                .collect(Collectors.toList());
    }

    private List<EntandoBundle> listBundlesFromEcr() {
        List<EntandoDeBundle> bundles;
        if (accessibleDigitalExchanges.isEmpty()) {
            bundles = k8SServiceClient.getBundlesInObservedNamespaces();
        } else {
            bundles = k8SServiceClient.getBundlesInNamespaces(accessibleDigitalExchanges);
        }

        return bundles.stream()
                .map(this::convertToBundleFromEcr)
                .collect(Collectors.toList());
    }

    @Override
    public EntandoBundle convertToBundleFromEntity(EntandoBundleEntity entity) {
        EntandoBundleJob installedJob = null;
        EntandoBundleJob lastJob = jobRepository.findFirstByComponentIdOrderByStartedAtDesc(entity.getId())
                .map(EntandoBundleJob::fromEntity)
                .orElse(null);

        if (installedComponentRepo.existsById(entity.getId())) {
            installedJob = jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(entity.getId(), JobStatus.INSTALL_COMPLETED)
                    .map(EntandoBundleJob::fromEntity)
                    .orElse(null);
        }

        return EntandoBundle.builder()
                .code(entity.getId())
                .title(entity.getName())
                .description(entity.getDescription())
                .thumbnail(entity.getImage())
                //.organization(entity.getOrganization())
                .componentTypes(entity.getType())
                .lastJob(lastJob)
                .installedJob(installedJob)
                //.versions() //DB entity shouldn't keep all available versions
                .build();
    }

    @Override
    public EntandoBundleEntity convertToEntityFromEcr(EntandoDeBundle bundle) {
        return convertToEntityFromBundle(convertToBundleFromEcr(bundle));
    }

    @Override
    public EntandoBundleEntity convertToEntityFromBundle(EntandoBundle bundle) {
        return EntandoBundleEntity.builder()
                .id(bundle.getCode())
                .name(bundle.getTitle())
                .name(bundle.getCode())
                .description(bundle.getDescription())
                .image(bundle.getThumbnail())
                //.organization(entity.getOrganization())
                .type(bundle.getComponentTypes())
                .installed(bundle.isInstalled())
                .version(bundle.isInstalled() ? bundle.getInstalledJob().getComponentVersion() : null)
                .lastUpdate(bundle.isInstalled() ?
                        Date.from(bundle.getLastJob().getFinishedAt().atZone(ZoneOffset.UTC).toInstant()) : null)
                .build();
    }

    @Override
    public EntandoBundle convertToBundleFromEcr(EntandoDeBundle bundle) {
        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        if (bundle.getMetadata().getLabels() != null) {
            bundle.getMetadata().getLabels()
                    .keySet().stream()
                    .filter(ComponentType::isValidType)
                    .forEach(bundleComponentTypes::add);
        }

        EntandoDeBundleDetails details = bundle.getSpec().getDetails();
        String code = bundle.getMetadata().getName();

        EntandoBundleJob installedJob = null;
        EntandoBundleJob lastJob = jobRepository.findFirstByComponentIdOrderByStartedAtDesc(code)
                .map(EntandoBundleJob::fromEntity)
                .orElse(null);

        if (installedComponentRepo.existsById(code)) {
            installedJob = jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(code, JobStatus.INSTALL_COMPLETED)
                    .map(EntandoBundleJob::fromEntity)
                    .orElse(null);
        }

        return EntandoBundle.builder()
                .code(code)
                .title(details.getName())
                .description(details.getDescription())
                .componentTypes(bundleComponentTypes)
                .thumbnail(details.getThumbnail())
                .installedJob(installedJob)
                .lastJob(lastJob)
                .versions(bundle.getSpec().getTags().stream()
                        .map(EntandoBundleVersion::fromEntity) //TODO how to read timestamp from k8s custom model?
                        .collect(Collectors.toList()))
                .build();
    }

}
