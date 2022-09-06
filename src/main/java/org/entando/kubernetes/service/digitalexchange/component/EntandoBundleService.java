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
import java.util.Optional;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity.EcrInstallCause;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;

public interface EntandoBundleService {

    /**
     * This method retrieves the EntandoBundles installed or removed from post-init operation.
     *
     * @return the list of EntandoBundles installed or removed from post-init operation
     */
    PagedMetadata<EntandoBundle> listInstalledOrRemovedPostInitBundles();

    PagedMetadata<EntandoBundle> listBundles();

    PagedMetadata<EntandoBundle> listBundles(PagedListRequest request);

    Optional<EntandoBundle> getInstalledBundle(String id);

    /**
     * This method retrieves the installed EntandoBundle data based on the input bundle identifier.
     *
     * @param bundleId the bundle identifier
     * @return the Optional EntandoBundle identified by the bundleId in input or Optional.empty()
     */
    Optional<EntandoBundle> getInstalledBundleByBundleId(String bundleId);

    List<EntandoBundleComponentJobEntity> getBundleInstalledComponents(String id);

    //Utility converters
    EntandoBundle convertToBundleFromEntity(EntandoBundleEntity entity);

    EntandoBundle convertToBundleFromEcr(EntandoDeBundle bundle);

    EntandoBundleEntity convertToEntityFromBundle(EntandoBundle bundle);

    EntandoBundleEntity convertToEntityFromEcr(EntandoDeBundle bundle);

    EntandoBundle deployDeBundle(BundleInfo bundleInfo);

    EntandoBundle deployDeBundle(BundleInfo bundleInfo, EcrInstallCause operator);

    String undeployDeBundle(String bundleName);

    BundlesStatusResult getBundlesStatus(List<String> bundlesUrlList);

    BundlesStatusItem getSingleBundleStatus(String bundleName);

    Optional<EntandoBundle> getBundleByRepoUrl(String repoUrl);

    /**
     * This method retrieves the installed EntandoBundle data based on the input encoded URL.
     *
     * @param encodedRepoUrl a valid URL encoded with base64 algorithm used to generate the bundleId
     * @return the Optional EntandoBundle identified by the encoded URL in input or Optional.empty()
     */
    Optional<EntandoBundle> getInstalledBundleByEncodedUrl(String encodedRepoUrl);

    /**
     * This method retrieves from entando k8s service the list of EntandoDeBundle CRs deployed (installed or not).
     *
     * @return the list of EntandoDeBundle CRs deployed (installed or not)
     */
    List<EntandoBundle> listBundlesFromEcr();
}