package org.entando.kubernetes.service.digitalexchange.component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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
    public BundlesStatusItem composeBundleStatusItemByURL(String url, List<EntandoBundleEntity> installedBundleEntities,
            List<EntandoBundle> deployedBundles, List<EntandoBundleEntity> installedButNotDeployed) {

        if (ObjectUtils.isEmpty(url)) {
            throw new EntandoComponentManagerException("The received URL is empty or null");
        }
        bundlesListsNotNullOrThrow(installedBundleEntities, deployedBundles, installedButNotDeployed);

        return composeBundleStatusItem(filterBundleEntityByUrl(url), filterBundleByUrl(url),
                    installedBundleEntities, deployedBundles, installedButNotDeployed)
                // otherwise return NOT FOUND
                .orElseGet(() -> new BundlesStatusItem(url, null, BundleStatus.NOT_FOUND, null));
    }

    /**
     * search the bundle corresponding to the received name in the received list, then return the corresponding
     * BundlesStatusItem search order: - installed but not deployed anymore - installed - deployed - otherwise not
     * found.
     *
     * @param name                    the name of the bundle to search
     * @param installedBundleEntities list of installed bundles
     * @param deployedBundles         list of deployed bundles
     * @param installedButNotDeployed list of installed but not deployed anymore bundles
     * @return the BundlesStatusItem resulting by the search
     */
    public BundlesStatusItem composeBundleStatusItemByName(String name,
            List<EntandoBundleEntity> installedBundleEntities,
            List<EntandoBundle> deployedBundles, List<EntandoBundleEntity> installedButNotDeployed) {

        if (ObjectUtils.isEmpty(name)) {
            throw new EntandoComponentManagerException("The received bundle name is empty or null");
        }

        return composeBundleStatusItem(filterBundleEntityByName(name), filterBundleByName(name),
                    installedBundleEntities, deployedBundles, installedButNotDeployed)
                // otherwise return NOT FOUND
                .orElseGet(() -> new BundlesStatusItem(null, name, BundleStatus.NOT_FOUND, null));
    }

    /**
     * search the bundle corresponding to the received predicates in the received list, then return the corresponding
     * BundlesStatusItem search order: - installed but not deployed anymore - installed - deployed - otherwise not
     * found.
     *
     * @param bundleEntityPredicate   the predicate to apply to the bundle entities list
     * @param bundlePredicate         the predicate to apply to the bundles list
     * @param installedBundleEntities list of installed bundles
     * @param deployedBundles         list of deployed bundles
     * @param installedButNotDeployed list of installed but not deployed anymore bundles
     * @return the BundlesStatusItem resulting by the search
     */
    public Optional<BundlesStatusItem> composeBundleStatusItem(Predicate<EntandoBundleEntity> bundleEntityPredicate,
            Predicate<EntandoBundle> bundlePredicate, List<EntandoBundleEntity> installedBundleEntities,
            List<EntandoBundle> deployedBundles, List<EntandoBundleEntity> installedButNotDeployed) {

        bundlesListsNotNullOrThrow(installedBundleEntities, deployedBundles, installedButNotDeployed);

        // search into installed but not deployed
        return composeBundleStatusItem(bundleEntityPredicate, installedButNotDeployed,
                BundleStatus.INSTALLED_NOT_DEPLOYED, EntandoBundleEntity::getVersion)
                // search into installed and deployed
                .or(() -> composeBundleStatusItem(bundleEntityPredicate, installedBundleEntities,
                        BundleStatus.INSTALLED, EntandoBundleEntity::getVersion))
                // search into not installed but deployed
                .or(() -> composeBundleStatusItem(bundlePredicate, deployedBundles, BundleStatus.DEPLOYED));
    }


    /**
     * search the received repoUrl in the list of EntandoBundleEntity. if it is present, return the BundlesStatusItem
     * corresponding to the received params.
     *
     * @param filterPredicate  the predicate to find the EntandoBundleEntity of interest
     * @param bundleEntityList the list of EntandoBundleEntity in which search the bundle with the url identified by
     *                         repoUrl
     * @param bundleStatus     the status to assign to the BundlesStatusItem to return if the repoUrl if found in the
     *                         list
     * @param versionGetFn     the function to execute to find the value to assign to the version property of the
     *                         BundlesStatusItem to return if the repoUrl if found in the list
     * @return an Optional containing the BundlesStatusItem corresponding to the received params if the repoUrl is found
     *          in the receive list, an empty one otherwise
     */
    private Optional<BundlesStatusItem> composeBundleStatusItem(Predicate<EntandoBundleEntity> filterPredicate,
            List<EntandoBundleEntity> bundleEntityList, BundleStatus bundleStatus,
            Function<EntandoBundleEntity, String> versionGetFn) {

        return bundleEntityList.stream()
                .filter(bundle -> !ObjectUtils.isEmpty(bundle.getRepoUrl()) && !ObjectUtils.isEmpty(bundle.getName()))
                .filter(filterPredicate)
                .findFirst()
                .map(bundle -> new BundlesStatusItem(bundle.getRepoUrl(), bundle.getName(), bundleStatus,
                        versionGetFn.apply(bundle)));
    }


    /**
     * search the received repoUrl in the list of EntandoBundle. if it is present, return the BundlesStatusItem
     * corresponding to the received params.
     *
     * @param filterPredicate the predicate to find the EntandoBundle of interest
     * @param bundleList      the list of EntandoBundle in which search the bundle with the url identified by repoUrl
     * @param bundleStatus    the status to assign to the BundlesStatusItem to return if the repoUrl if found in the
     *                        list
     * @return an Optional containing the BundlesStatusItem corresponding to the received params if the repoUrl is found
     *          in the receive list, an empty one otherwise
     */
    private Optional<BundlesStatusItem> composeBundleStatusItem(Predicate<EntandoBundle> filterPredicate,
            List<EntandoBundle> bundleList, BundleStatus bundleStatus) {

        return bundleList.stream()
                .filter(bundle -> !ObjectUtils.isEmpty(bundle.getRepoUrl()) && !ObjectUtils.isEmpty(bundle.getCode()))
                .filter(filterPredicate)
                .findFirst()
                .map(bundle -> new BundlesStatusItem(bundle.getRepoUrl(), bundle.getCode(), bundleStatus, null));
    }


    /**
     * if any of the received lists is null, throw an EntandoComponentManagerException.
     *
     * @param installedBundleEntities a list of installed EntandoBundleEntity
     * @param deployedBundles         a list of deployed EntandoBundle
     * @param installedButNotDeployed a list of installed but not deployed EntandoBundleEntity
     */
    private void bundlesListsNotNullOrThrow(List<EntandoBundleEntity> installedBundleEntities,
            List<EntandoBundle> deployedBundles, List<EntandoBundleEntity> installedButNotDeployed) {

        if (installedBundleEntities == null) {
            throw new EntandoComponentManagerException("The list of installed bundles is null");
        }
        if (deployedBundles == null) {
            throw new EntandoComponentManagerException("The list of deployed bundles is null");
        }
        if (installedButNotDeployed == null) {
            throw new EntandoComponentManagerException("The list of installed but not deployed bundles is null");
        }
    }


    private Predicate<EntandoBundleEntity> filterBundleEntityByUrl(String repoUrl) {
        return bundleEntity -> bundleEntity.getRepoUrl().equals(repoUrl);
    }

    private Predicate<EntandoBundle> filterBundleByUrl(String repoUrl) {
        return bundle -> bundle.getRepoUrl().equals(repoUrl);
    }

    private Predicate<EntandoBundleEntity> filterBundleEntityByName(String name) {
        return bundleEntity -> bundleEntity.getName().equals(name);
    }

    private Predicate<EntandoBundle> filterBundleByName(String name) {
        return bundle -> bundle.getCode().equals(name);
    }
}
