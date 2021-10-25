package org.entando.kubernetes.service.digitalexchange.component;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.springframework.stereotype.Component;

@Component
public class BundleStatusHelper {

    /**
     * search the bundle corresponding to the received url in the received list, then return the corresponding
     * BundlesStatusItem search order: - installed but not deployed anymore - installed - deployed - otherwise not
     * found.
     *
     * @param url                     the url of the bundle to search
     * @param installedBundleEntities list of installed bundles
     * @param deployedBundles         list of deployed bundles
     * @param installedButNotDeployed list of installed but not deployed anymore bundles
     * @return the BundlesStatusItem resulting by the search
     */
    public BundlesStatusItem composeBundleStatusItem(URL url, List<EntandoBundleEntity> installedBundleEntities,
            List<EntandoBundle> deployedBundles, List<EntandoBundleEntity> installedButNotDeployed) {

        if (url == null) {
            throw new EntandoComponentManagerException("The received URL is empty or null");
        }
        if (installedBundleEntities == null) {
            throw new EntandoComponentManagerException("The list of installed bundles is null");
        }
        if (deployedBundles == null) {
            throw new EntandoComponentManagerException("The list of deployed bundles is null");
        }
        if (installedButNotDeployed == null) {
            throw new EntandoComponentManagerException("The list of installed but not deployed bundles is null");
        }

        // search into installed but not deployed
        return composeBundleStatusItem(url, installedButNotDeployed,
                BundleStatus.INSTALLED_NOT_DEPLOYED, EntandoBundleEntity::getVersion)
                // search into installed and deployed
                .or(() -> composeBundleStatusItem(url, installedBundleEntities, BundleStatus.INSTALLED,
                        EntandoBundleEntity::getVersion))
                // search into not installed but deployed
                .or(() -> composeBundleStatusItem(url, deployedBundles, BundleStatus.DEPLOYED))
                // otherwise return NOT FOUND
                .orElseGet(() -> new BundlesStatusItem(url.toString(), BundleStatus.NOT_FOUND, null));
    }

    /**
     * search the received repoUrl in the list of EntandoBundleEntity. if it is present, return the BundlesStatusItem
     * corresponding to the received params.
     *
     * @param repoUrl          the repo url identifying the bundle to search
     * @param bundleEntityList the list of EntandoBundleEntity in which search the bundle with the url identified by
     *                         repoUrl
     * @param bundleStatus     the status to assign to the BundlesStatusItem to return if the repoUrl if found in the
     *                         list
     * @param versionGetFn     the function to execute to find the value to assign to the version property of the
     *                         BundlesStatusItem to return if the repoUrl if found in the list
     * @return an Optional containing the BundlesStatusItem corresponding to the received params if the repoUrl is found
     *          in the receive list, an empty one otherwise
     */
    private Optional<BundlesStatusItem> composeBundleStatusItem(URL repoUrl,
            List<EntandoBundleEntity> bundleEntityList, BundleStatus bundleStatus,
            Function<EntandoBundleEntity, String> versionGetFn) {

        return bundleEntityList.stream()
                .filter(bundle -> !ObjectUtils.isEmpty(bundle.getRepoUrl()))
                .filter(bundle -> bundle.getRepoUrl().toString().equals(repoUrl.toString()))
                .findFirst()
                .map(bundle -> new BundlesStatusItem(repoUrl.toString(), bundleStatus, versionGetFn.apply(bundle)));
    }

    /**
     * search the received repoUrl in the list of EntandoBundle. if it is present, return the BundlesStatusItem
     * corresponding to the received params.
     *
     * @param repoUrl      the repo url identifying the bundle to search
     * @param bundleList   the list of EntandoBundle in which search the bundle with the url identified by repoUrl
     * @param bundleStatus the status to assign to the BundlesStatusItem to return if the repoUrl if found in the list
     * @return an Optional containing the BundlesStatusItem corresponding to the received params if the repoUrl is found
     *          in the receive list, an empty one otherwise
     */
    private Optional<BundlesStatusItem> composeBundleStatusItem(URL repoUrl, List<EntandoBundle> bundleList,
            BundleStatus bundleStatus) {

        return bundleList.stream()
                .filter(bundle -> !ObjectUtils.isEmpty(bundle.getRepoUrl()))
                .filter(bundle -> bundle.getRepoUrl().equals(repoUrl.toString()))
                .findFirst()
                .map(bundle -> new BundlesStatusItem(repoUrl.toString(), bundleStatus, null));
    }
}
