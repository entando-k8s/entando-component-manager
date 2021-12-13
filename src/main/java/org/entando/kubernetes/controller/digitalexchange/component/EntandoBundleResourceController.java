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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.DeletedObjectResponse;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EntandoBundleResourceController implements EntandoBundleResource {

    private final EntandoBundleService bundleService;
    private final EntandoBundleComponentUsageService usageService;
    private static final String REPO_URL_PATH_PARAM = "repoUrl";

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
    public ResponseEntity<SimpleRestResponse<DeletedObjectResponse>> undeployBundle(String component) {
        final String deleteRegistryName = bundleService.undeployDeBundle(component);
        return ResponseEntity.ok(new SimpleRestResponse<>(new DeletedObjectResponse(deleteRegistryName)));
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
    public ResponseEntity<SimpleRestResponse<BundlesStatusResult>> getBundlesStatusByRepoUrl(
            BundlesStatusQuery bundlesStatusQuery) {

        if (bundlesStatusQuery == null || ObjectUtils.isEmpty(bundlesStatusQuery.getIds())) {
            return ResponseEntity.ok(new SimpleRestResponse<>(new BundlesStatusResult()));
        }

        List<BundlesStatusItem> invalidBundlesStatusItemList = new ArrayList<>();
        List<String> repoUrlList = new ArrayList<>();

        for (String stringUrl : bundlesStatusQuery.getIds()) {
            try {
                ValidationFunctions.composeUrlForcingHttpProtocolOrThrow(stringUrl,
                        "The received url is empty", "The received url is not valid");
                repoUrlList.add(stringUrl);
            } catch (Exception e) {
                log.error("Invalid URL received: {} - it will be skipped in the search", stringUrl);
                invalidBundlesStatusItemList.add(
                        new BundlesStatusItem(stringUrl, null, BundleStatus.INVALID_REPO_URL, null));
            }
        }

        BundlesStatusResult bundlesStatusResult = bundleService.getBundlesStatus(repoUrlList);

        if (! invalidBundlesStatusItemList.isEmpty()) {
            bundlesStatusResult.getBundlesStatuses().addAll(invalidBundlesStatusItemList);
        }

        return ResponseEntity.ok(new SimpleRestResponse<>(bundlesStatusResult));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<BundlesStatusItem>> getSingleBundleStatusByName(String component) {

        if (ObjectUtils.isEmpty(component)) {
            return ResponseEntity.ok(new SimpleRestResponse<>(new BundlesStatusItem()));
        }

        BundlesStatusItem bundlesStatusItem = bundleService.getSingleBundleStatus(component);

        return ResponseEntity.ok(new SimpleRestResponse<>(bundlesStatusItem));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundle>> getBundleByRestNamedId(RestNamedId id) {

        final String repoUrl = id.getValidValue(REPO_URL_PATH_PARAM).orElseThrow(() ->
                Problem.valueOf(Status.BAD_REQUEST, "Can't manage a request with id: " + id));

        Optional<EntandoBundle> entandoBundle = bundleService.getBundleByRepoUrl(repoUrl);
        if (entandoBundle.isPresent()) {
            return ResponseEntity.ok(new SimpleRestResponse<>(entandoBundle.get()));
        } else {
            return new ResponseEntity<>(new SimpleRestResponse<>(new EntandoBundle()), HttpStatus.NOT_FOUND);
        }
    }
}
