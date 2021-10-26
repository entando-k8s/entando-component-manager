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

package org.entando.kubernetes.controller.digitalexchange.component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EntandoBundleResourceController implements EntandoBundleResource {

    private final EntandoBundleService bundleService;
    private final EntandoBundleComponentUsageService usageService;

    @Override
    public ResponseEntity<PagedRestResponse<EntandoBundle>> getBundles(PagedListRequest requestList) {
        PagedMetadata<EntandoBundle> pagedBundles = bundleService.listBundles(requestList);
        PagedRestResponse<EntandoBundle> response = new PagedRestResponse<>(pagedBundles);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundle>> deployBundle(BundleInfo bundleInfo) {
        final EntandoBundle entandoBundle = bundleService.deployDeBundle(bundleInfo);
        return ResponseEntity.ok(new SimpleRestResponse<>(entandoBundle));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<List<EntandoCoreComponentUsage>>> getBundleUsageSummary(String component) {
        //I should be able to retrieve the related installed components given component id
        List<EntandoBundleComponentJobEntity> bundleInstalledComponents = bundleService
                .getBundleInstalledComponents(component);
        //For each installed components, I should check the summary
        List<EntandoCoreComponentUsage> usageList = bundleInstalledComponents.stream()
                .map(cj -> usageService.getUsage(cj.getComponentType(), cj.getComponentId()))
                .filter(u -> !(u instanceof IrrelevantComponentUsage))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new SimpleRestResponse<>(usageList));
    }

    @Override
    @PostMapping(value = "/status/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<BundlesStatusResult>> getBundlesStatus(
            BundlesStatusQuery bundlesStatusQuery) {

        if (bundlesStatusQuery == null || ObjectUtils.isEmpty(bundlesStatusQuery.getIds())) {
            return ResponseEntity.ok(new SimpleRestResponse<>(new BundlesStatusResult()));
        }

        List<BundlesStatusItem> invalidBundlesStatusItemList = new ArrayList<>();
        List<URL> repoUrlList = new ArrayList<>();

        for (String stringUrl : bundlesStatusQuery.getIds()) {
            try {
                URL url = ValidationFunctions.composeUrlOrThrow(stringUrl, "The received url is empty", "The received url is not valid");
                repoUrlList.add(url);
            } catch (Exception e) {
                log.error("Invalid URL received: {} - it will be skipped in the search", stringUrl);
                invalidBundlesStatusItemList.add(new BundlesStatusItem(stringUrl, BundleStatus.INVALID_REPO_URL, null));
            }
        }

        BundlesStatusResult bundlesStatusResult = bundleService.getBundlesStatus(repoUrlList);

        if (! invalidBundlesStatusItemList.isEmpty()) {
            bundlesStatusResult.getBundlesStatuses().addAll(invalidBundlesStatusItemList);
        }

        return ResponseEntity.ok(new SimpleRestResponse<>(bundlesStatusResult));
    }
}
