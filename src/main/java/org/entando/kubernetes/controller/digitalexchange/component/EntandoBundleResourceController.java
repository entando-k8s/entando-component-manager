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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundleUsageSummary;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EntandoBundleResourceController implements EntandoBundleResource {

    private final EntandoBundleService bundleService;

    @Override
    public ResponseEntity<PagedRestResponse<EntandoBundle>> getBundles() {
        List<EntandoBundle> entandoBundles = bundleService.getComponents();
        PagedMetadata<EntandoBundle> pagedMetadata =
                new PagedMetadata<>(1, 100, 1, entandoBundles.size());
        pagedMetadata.setBody(entandoBundles);
        PagedRestResponse<EntandoBundle> response = new PagedRestResponse<>(pagedMetadata);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EntandoBundleUsageSummary> getBundleUsageSummary(String component) {
        //Given an installed component id
        EntandoBundle installedComponent = bundleService.getInstalledComponent(component)
                .orElseThrow(() -> new BundleNotInstalledException("Component " + component + " is not installed"));
        //I should be able to retrieve the related installed components
        List<EntandoBundleComponentJob> bundleInstalledComponents = bundleService.getBundleInstalledComponents(component);
//        List<DigitalExchangeJobComponent> bundleInstalledComponents = componentsService.getBundleInstalledComponents(component);
        //For each installed components, I should check the summary
//        for(DigitalExchangeJobComponent c: bundleInstalledComponents) {
//
//        }

        return ResponseEntity.ok(new EntandoBundleUsageSummary());
    }
}
