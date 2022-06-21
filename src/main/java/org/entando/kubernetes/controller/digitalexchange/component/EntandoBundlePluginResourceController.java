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

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundlePluginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EntandoBundlePluginResourceController implements EntandoBundlePluginResource {

    private final EntandoBundlePluginService bundleComponentService;
    private static final String ERROR_MSG = "Can't manage a request with bundleCode: ";

    @Override
    public ResponseEntity<PagedRestResponse<PluginData>> getBundleInstalledComponents(
            RestNamedId bundleId,
            PagedListRequest requestList) {

        PagedMetadata<PluginData> pagedComponents = bundleId.getValidValue(REPO_URL_PATH_PARAM)
                .map(
                        encodedUrl -> bundleComponentService.getInstalledPluginsByEncodedUrl(requestList,
                                encodedUrl))
                .or(() -> {
                    if (StringUtils.contains(bundleId.toString(), RestNamedId.SEPARATOR)) {
                        throw Problem.valueOf(Status.NOT_FOUND, ERROR_MSG + bundleId);
                    } else {
                        return Optional.of(
                                bundleComponentService.getInstalledPluginsByBundleId(requestList,
                                        bundleId.toString()));
                    }
                }).orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, ERROR_MSG + bundleId));

        PagedRestResponse<PluginData> response = new PagedRestResponse<>(pagedComponents);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PluginData> getBundleInstalledPlugin(RestNamedId bundleId, String pluginName) {
        PluginData plugin = bundleId.getValidValue(REPO_URL_PATH_PARAM)
                .map(
                        encodedUrl -> bundleComponentService.getInstalledPluginByEncodedUrl(encodedUrl,
                                pluginName))
                .or(() -> {
                    if (StringUtils.contains(bundleId.toString(), RestNamedId.SEPARATOR)) {
                        throw Problem.valueOf(Status.NOT_FOUND, ERROR_MSG + bundleId);
                    } else {
                        return Optional.of(
                                bundleComponentService.getInstalledPlugin(bundleId.toString(), pluginName));
                    }
                }).orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, ERROR_MSG + bundleId));

        return ResponseEntity.ok(plugin);
    }

}