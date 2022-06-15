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

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;


@Service
@Slf4j
@AllArgsConstructor
public class EntandoBundleComponentServiceImpl implements EntandoBundleComponentService {

    private final EntandoBundleComponentJobRepository jobComponentRepository;

    private final EntandoBundleService bundleService;


    @Override
    public PagedMetadata<EntandoBundleComponentJob> getInstalledComponentsByBundleCode(PagedListRequest requestList,
            String bundleCode) {

        // FIXME wait bundleCode validator from Luca
        // throw EntandoValidationException
        try {
            EntandoBundle bundle = bundleService.getInstalledBundle(bundleCode)
                    .orElseThrow(() -> new BundleNotInstalledException(
                            "Bundle " + bundleCode + " is not installed in the system"));

            // FIXME to replace with Luca's new tabels
            List<EntandoBundleComponentJob> allComponents = jobComponentRepository.findAllByParentJobId(
                            bundle.getInstalledJob().getId()).stream().map(EntandoBundleComponentJob::fromEntity)
                    .collect(Collectors.toList());
            List<EntandoBundleComponentJob> filteredComponents = new EntandoBundleComponentJobListProcessor(requestList,
                    allComponents).filterAndSort().toList();
            List<EntandoBundleComponentJob> sublist = requestList.getSublist(filteredComponents);

            PagedMetadata<EntandoBundleComponentJob> pagedComponents = new PagedMetadata<>(requestList, sublist,
                    filteredComponents.size());

            return pagedComponents;
        } catch (IllegalArgumentException ex) {
            throw Problem.valueOf(Status.NOT_FOUND,
                    String.format("Can't manage a request with bundleCode: %s - exception: %s", bundleCode,
                            ex.getMessage()));
        }
    }

    @Override
    public PagedMetadata<EntandoBundleComponentJob> getInstalledComponentsByEncodedUrl(PagedListRequest requestList,
            String encodedUrl) {

        try {
            EntandoBundle bundle = bundleService.getInstalledBundleByEncodedUrl(encodedUrl)
                    .orElseThrow(() -> new BundleNotInstalledException(
                            "Bundle " + encodedUrl + " is not installed in the system"));

            return this.getInstalledComponentsByBundleCode(requestList, bundle.getCode());

        } catch (IllegalArgumentException ex) {
            throw Problem.valueOf(Status.NOT_FOUND,
                    String.format("Can't manage a request with encodedUrl: %s - exception: %s", encodedUrl,
                            ex.getMessage()));
        }


    }

}
