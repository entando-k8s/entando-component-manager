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

import java.net.URL;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EntandoBundleServiceImpl implements EntandoBundleService {

    private final K8SServiceClient k8SServiceClient;
    private final List<String> accessibleDigitalExchanges;
    private final EntandoBundleJobRepository jobRepository;
    private final InstalledEntandoBundleRepository installedComponentRepo;
    private final EntandoBundleComponentJobRepository jobComponentRepository;
    private final BundleStatusHelper bundleStatusHelper;

    public EntandoBundleServiceImpl(K8SServiceClient k8SServiceClient,
            @Value("${entando.component.repository.namespaces:}") List<String> accessibleDigitalExchanges,
            EntandoBundleJobRepository jobRepository,
            EntandoBundleComponentJobRepository jobComponentRepository,
            InstalledEntandoBundleRepository installedComponentRepo,
            BundleStatusHelper bundleStatusHelper) {

        this.k8SServiceClient = k8SServiceClient;
        this.accessibleDigitalExchanges = Optional.ofNullable(accessibleDigitalExchanges).orElse(new ArrayList<>())
                .stream().filter(Strings::isNotBlank).collect(Collectors.toList());
        this.jobRepository = jobRepository;
        this.jobComponentRepository = jobComponentRepository;
        this.installedComponentRepo = installedComponentRepo;
        this.bundleStatusHelper = bundleStatusHelper;
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
        List<EntandoBundle> installedButNotAvailableOnEcr = filterInstalledButNotAvailableOnEcrToEntandoBundle(
                availableBundles, installedBundles);

        allComponents.addAll(availableBundles);
        allComponents.addAll(installedButNotAvailableOnEcr);

        return updateCustomInstallationFlag(allComponents, installedBundles);
    }

    /**
     * for each installed bundle, update the field custom_installation in the allBundles list.
     *
     * @param allBundles       the list representing all the available EntandoBundle
     * @param installedBundles the list of installed EntandoBundleEntity
     * @return a new instance of a list of all bundles with customInstallation field updated
     */
    private List<EntandoBundle> updateCustomInstallationFlag(List<EntandoBundle> allBundles,
            List<EntandoBundleEntity> installedBundles) {

        // get installed bundles jobs id
        Set<UUID> installedBunblesJobIdSet = installedBundles.stream()
                .map(entandoBundleEntity -> entandoBundleEntity.getJob().getId())
                .collect(Collectors.toSet());

        // fetch EntandoBundleJobEntitis from DB for installed bundles
        Map<String, EntandoBundleJobEntity> installedBundleJobEntities =
                jobRepository.findEntandoBundleJobEntityByIdIn(installedBunblesJobIdSet)
                        .orElse(new ArrayList<>())
                        .stream()
                        .collect(Collectors.toMap(
                                EntandoBundleJobEntity::getComponentId,
                                entandoBundleEntity -> entandoBundleEntity));

        // populate and return customInstallation field
        return allBundles.stream().map(entandoBundle -> {
            if (installedBundleJobEntities.containsKey(entandoBundle.getCode())) {
                entandoBundle.setCustomInstallation(
                        installedBundleJobEntities.get(entandoBundle.getCode()).getCustomInstallation());
            }
            return entandoBundle;
        }).collect(Collectors.toList());
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
        if (bundle.getInstalledJob() != null && bundle.getInstalledJob().getStatus()
                .equals(JobStatus.INSTALL_COMPLETED)) {
            return jobComponentRepository.findAllByParentJobId(bundle.getInstalledJob().getId());
        } else {
            throw new EntandoComponentManagerException("Bundle " + id + " is not installed correctly");
        }
    }

    private List<EntandoBundle> filterInstalledButNotAvailableOnEcrToEntandoBundle(List<EntandoBundle> availableBundles,
            List<EntandoBundleEntity> installedBundles) {

        return filterInstalledButNotAvailableOnEcr(availableBundles, installedBundles).stream()
                .map(this::convertToBundleFromEntity)
                .collect(Collectors.toList());
    }

    private List<EntandoBundleEntity> filterInstalledButNotAvailableOnEcr(List<EntandoBundle> availableBundles,
            List<EntandoBundleEntity> installedBundles) {

        //TODO could be a problem if available bundles list is too big
        Set<String> keySet = availableBundles.stream().map(EntandoBundle::getCode).collect(Collectors.toSet());
        return installedBundles.stream()
                .filter(b -> !keySet.contains(b.getId()))
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
            installedJob = jobRepository
                    .findFirstByComponentIdAndStatusOrderByStartedAtDesc(entity.getId(), JobStatus.INSTALL_COMPLETED)
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
    public BundlesStatusResult getBundlesStatus(List<URL> repoUrlList) {

        List<EntandoBundleEntity> installedBundleEntities = installedComponentRepo.findAllByRepoUrlIn(repoUrlList);

        List<EntandoBundle> deployedBundles = listBundlesFromEcr();
        List<EntandoBundleEntity> installedButNotDeployed = filterInstalledButNotAvailableOnEcr(deployedBundles,
                installedBundleEntities);

        final List<BundlesStatusItem> bundleStatusList = repoUrlList.stream()
                .map(url -> bundleStatusHelper.composeBundleStatusItem(url, installedBundleEntities, deployedBundles,
                        installedButNotDeployed))
                .collect(Collectors.toList());

        return new BundlesStatusResult(bundleStatusList);
    }


    @Override
    public EntandoBundleEntity convertToEntityFromBundle(EntandoBundle bundle) {
        return EntandoBundleEntity.builder()
                .id(bundle.getCode())
                //.name(bundle.getTitle())
                .name(bundle.getCode())
                .description(bundle.getDescription())
                .image(bundle.getThumbnail())
                //.organization(entity.getOrganization())
                .type(bundle.getComponentTypes())
                .installed(bundle.isInstalled())
                .version(bundle.isInstalled() ? bundle.getInstalledJob().getComponentVersion() : null)
                .lastUpdate(bundle.isInstalled()
                        ? Date.from(bundle.getInstalledJob().getFinishedAt().atZone(ZoneOffset.UTC).toInstant()) : null)
                .repoUrl(bundle.getRepoUrlAsURL())
                .build();
    }

    @Override
    public EntandoBundle convertToBundleFromEcr(EntandoDeBundle deBundle) {
        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        BundleType bundleType = BundleType.STANDARD_BUNDLE;
        if (deBundle.getMetadata().getLabels() != null) {

            deBundle.getMetadata().getLabels()
                    .keySet().stream()
                    .filter(ComponentType::isValidType)
                    .forEach(bundleComponentTypes::add);

            bundleType = BundleUtilities.extractBundleTypeFromBundle(deBundle);
        }

        EntandoDeBundleDetails details = deBundle.getSpec().getDetails();
        String code = deBundle.getMetadata().getName();

        EntandoBundleJob installedJob = null;
        EntandoBundleJob lastJob = jobRepository.findFirstByComponentIdOrderByStartedAtDesc(code)
                .map(EntandoBundleJob::fromEntity)
                .orElse(null);

        if (installedComponentRepo.existsById(code)) {
            installedJob = jobRepository
                    .findFirstByComponentIdAndStatusOrderByStartedAtDesc(code, JobStatus.INSTALL_COMPLETED)
                    .map(EntandoBundleJob::fromEntity)
                    .orElse(null);
        }

        EntandoBundle bundle = EntandoBundle.builder()
                .code(code)
                .title(details.getName())
                .description(details.getDescription())
                .bundleType(bundleType)
                .componentTypes(bundleComponentTypes)
                .thumbnail(details.getThumbnail())
                .installedJob(installedJob)
                .lastJob(lastJob)
                .versions(deBundle.getSpec().getTags().stream()
                        .map(EntandoBundleVersion::fromEntity) //TODO how to read timestamp from k8s custom model?
                        .collect(Collectors.toList()))
                .build();

        final EntandoBundleVersion latestVersionFromDistTag = findLatestVersionFromDistTag(deBundle);
        bundle.setLatestVersion(latestVersionFromDistTag);

        bundle.setRepoUrl(getLatestTagRepoUrl(deBundle, latestVersionFromDistTag));

        return bundle;
    }

    /**
     * identify and return the latest version from distTag property of the received EntandoDeBundle.
     *
     * @param deBundle the EntandoDeBundle of which identify and return the latest version
     * @return the latest version shaped as EntandoBundleVersion
     */
    private EntandoBundleVersion findLatestVersionFromDistTag(EntandoDeBundle deBundle) {

        EntandoDeBundleDetails details = deBundle.getSpec().getDetails();

        if (details != null && details.getDistTags() != null && details.getDistTags()
                .containsKey(BundleUtilities.LATEST_VERSION)) {

            return new EntandoBundleVersion()
                    .setVersion(details.getDistTags().get(BundleUtilities.LATEST_VERSION).toString());
        } else {
            return BundleUtilities.composeLatestVersionFromDistTags(deBundle).orElse(null);
        }
    }

    /**
     * identify and return the repo url of the latest tag (semver) from the received EntandoDeBundle.
     *
     * @param deBundle                 the EntandoDeBundle of which identify and return the repo url of latest tag
     * @param latestVersionFromDistTag the latest version taken from distTag property
     * @return the latest repo url of latest tag
     */
    private String getLatestTagRepoUrl(EntandoDeBundle deBundle, EntandoBundleVersion latestVersionFromDistTag) {

        final List<EntandoDeBundleTag> tags = deBundle.getSpec().getTags();

        if (tags == null) {
            return null;
        }

        final Map<String, EntandoDeBundleTag> tagsMap = tags.stream()
                .collect(Collectors.toMap(
                        EntandoDeBundleTag::getVersion,
                        tag -> tag));

        // if the latestVersionFromDistTag is availabe, let's use that one
        if (latestVersionFromDistTag != null && tagsMap.containsKey(latestVersionFromDistTag.getVersion())) {
            return tagsMap.get(latestVersionFromDistTag.getVersion()).getTarball();
        }

        // otherwise let's calculate the latest from the tags list
        Optional<EntandoBundleVersion> latestVersionOpt = tags.stream()
                .map(tag -> new EntandoBundleVersion().setVersion(tag.getVersion()))
                .max(Comparator.comparing(EntandoBundleVersion::getSemVersion));

        if (latestVersionOpt.isEmpty()) {
            return null;
        }

        return tagsMap.get(latestVersionOpt.get().getVersion()).getTarball();
    }

    @Override
    public EntandoBundle deployDeBundle(EntandoDeBundle entandoDeBundle) {

        if (entandoDeBundle == null) {
            throw new InvalidBundleException("The received EntandoDeBundle to deploy is null");
        }

        EntandoDeBundle deployedBundle = k8SServiceClient.deployDeBundle(entandoDeBundle);
        return convertToBundleFromEcr(deployedBundle);
    }
}
