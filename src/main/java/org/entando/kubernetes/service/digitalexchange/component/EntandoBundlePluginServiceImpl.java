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
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleData;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Service
@Slf4j
@AllArgsConstructor
public class EntandoBundlePluginServiceImpl implements EntandoBundlePluginService {

    private final PluginDataRepository pluginDataRepository;
    private final InstalledEntandoBundleRepository installedEntandoBundleRepository;

    private final EntandoBundleService bundleService;

    @Override
    public PagedMetadata<EntandoBundleData> listBundles(PagedListRequest request) {
        // installed
        List<EntandoBundleData> installedBundles = installedEntandoBundleRepository.findAll()
                .stream().map(EntandoBundleData::fromEntity).collect(Collectors.toList());
        // CRD
        List<EntandoBundleData> entandoDeBundles = bundleService.listBundlesFromEcr().stream()
                .map(this::convertToEntandoBundleData).collect(Collectors.toList());

        // installed + CRD not installed
        List<EntandoBundleData> allBundles = new ArrayList<>(installedBundles);
        allBundles.addAll(entandoDeBundles.stream().filter(f -> !listContainsBundleByCode(installedBundles, f))
                .collect(Collectors.toList()));

        // filter all
        List<EntandoBundleData> localFilteredList = new EntandoBundleDataListProcessor(request, allBundles)
                .filterAndSort().toList();
        List<EntandoBundleData> sublist = request.getSublist(localFilteredList);

        return new PagedMetadata<>(request, sublist, localFilteredList.size());
    }

    private boolean listContainsBundleByCode(List<EntandoBundleData> allBundles, EntandoBundleData bundle) {
        return allBundles.stream().anyMatch(b -> StringUtils.equals(b.getBundleCode(), bundle.getBundleCode()));
    }

    @Override
    public PagedMetadata<PluginData> getInstalledPluginsByBundleId(PagedListRequest requestList,
            String bundleId) {

        isInstalledOrThrowError(bundleId);

        List<PluginData> allComponents = pluginDataRepository.findAllByBundleId(bundleId).stream()
                .map(PluginData::fromEntity)
                .collect(Collectors.toList());
        List<PluginData> sublist = requestList.getSublist(allComponents);

        return new PagedMetadata<>(requestList, sublist, allComponents.size());
    }

    @Override
    public PagedMetadata<PluginData> getInstalledPluginsByEncodedUrl(PagedListRequest requestList,
            String encodedUrl) {

        try {
            String decodedUrl = BundleUtilities.decodeUrl(encodedUrl);
            String bundleId = BundleUtilities.removeProtocolAndGetBundleId(
                    ValidationFunctions.composeCommonUrlOrThrow(decodedUrl, "", ""));

            return this.getInstalledPluginsByBundleId(requestList, bundleId);

        } catch (IllegalArgumentException ex) {
            throw Problem.valueOf(Status.NOT_FOUND,
                    String.format("Can't manage a request with encodedUrl: %s - exception: %s", encodedUrl,
                            ex.getMessage()));
        }
    }

    @Override
    public PluginData getInstalledPlugin(String bundleId, String pluginName) {
        isInstalledOrThrowError(bundleId);

        return pluginDataRepository.findByBundleIdAndPluginName(bundleId, pluginName)
                .map(PluginData::fromEntity)
                .orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND,
                        String.format("Can't find plugin with  bundleId: %s - : %s", bundleId, pluginName)));
    }

    @Override
    public PluginData getInstalledPluginByEncodedUrl(String encodedUrl, String pluginName) {
        try {
            String decodedUrl = BundleUtilities.decodeUrl(encodedUrl);
            String bundleId = BundleUtilities.removeProtocolAndGetBundleId(
                    ValidationFunctions.composeCommonUrlOrThrow(decodedUrl, "", ""));

            return this.getInstalledPlugin(bundleId, pluginName);

        } catch (IllegalArgumentException ex) {
            throw Problem.valueOf(Status.NOT_FOUND,
                    String.format("Can't manage a request with encodedUrl: %s - exception: %s", encodedUrl,
                            ex.getMessage()));
        }
    }

    private boolean isInstalledOrThrowError(String bundleId) {
        return bundleService.getInstalledBundleByBundleId(bundleId).map(e -> true)
                .orElseThrow(() -> new BundleNotInstalledException(
                        "Bundle " + bundleId + " is not installed in the system"));
    }

    private EntandoBundleData convertToEntandoBundleData(EntandoBundle bundle) {
        return EntandoBundleData.builder()
                .id(null)
                .bundleId(BundleUtilities.removeProtocolAndGetBundleId(bundle.getRepoUrl()))
                .bundleCode(bundle.getCode())
                .installed(bundle.isInstalled())
                .componentTypes(bundle.getComponentTypes())
                .publicationUrl(bundle.getRepoUrl())
                .build();
    }
}
