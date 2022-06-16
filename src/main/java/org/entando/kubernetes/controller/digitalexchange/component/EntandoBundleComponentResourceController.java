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
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EntandoBundleComponentResourceController implements EntandoBundleComponentResource {

    private final EntandoBundleComponentService bundleComponentService;

    // FIXME 404 ??? exception fo empty list ? NO
    // FIXME manage Accepet headers with version content negotiation
    @Override
    public ResponseEntity<PagedRestResponse<EntandoBundleComponentJob>> getBundleInstalledComponents(
            RestNamedId bundleCode,
            PagedListRequest requestList) {

        PagedMetadata<EntandoBundleComponentJob> pagedComponents = bundleCode.getValidValue(REPO_URL_PATH_PARAM)
                .map(
                        encodedUrl -> bundleComponentService.getInstalledComponentsByEncodedUrl(requestList,
                                encodedUrl))
                .or(() -> {
                    if (StringUtils.contains(bundleCode.toString(), RestNamedId.SEPARATOR)) {
                        throw Problem.valueOf(Status.NOT_FOUND,
                                "Can't manage a request with bundleCode: " + bundleCode);
                    } else {
                        return Optional.of(
                                bundleComponentService.getInstalledComponentsByBundleCode(requestList,
                                        bundleCode.toString()));
                    }
                }).orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND,
                        "Can't manage a request with bundleCode: " + bundleCode));
        // FIXME empty list is error? not for only assets ...
        //        if (pagedComponents.getBody().isEmpty()) {
        //            throw Problem.valueOf(Status.UNPROCESSABLE_ENTITY, "Bundle " + bundleCode + " is not installed correctly");
        //        } else {
        //        }
        PagedRestResponse<EntandoBundleComponentJob> response = new PagedRestResponse<>(pagedComponents);
        return ResponseEntity.ok(response);
    }

}
