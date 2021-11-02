package org.entando.kubernetes.service.digitalexchange.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BundleStatusHelperTest {

    private final List<EntandoBundleEntity> installedButNotDeployed = List.of(
            new EntandoBundleEntity()
                    .setRepoUrl(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED)
                    .setName(BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED)
                    .setVersion(BundleStatusItemStubHelper.INSTALLED_VERSION_INSTALLED_NOT_DEPLOYED),
            new EntandoBundleEntity().setRepoUrl("http://www.myfakebundle1.com").setVersion("v1.0.0")
    );
    private final List<EntandoBundleEntity> installedBundleEntities = List.of(
            new EntandoBundleEntity().setRepoUrl(BundleStatusItemStubHelper.ID_INSTALLED)
                    .setName(BundleStatusItemStubHelper.NAME_INSTALLED)
                    .setVersion(BundleStatusItemStubHelper.INSTALLED_VERSION_INSTALLED),
            new EntandoBundleEntity().setRepoUrl("http://www.myfakebundle2.com").setVersion("v2.0.0")
    );
    private final List<EntandoBundle> deployedBundles = List.of(
            new EntandoBundle().setRepoUrl(BundleStatusItemStubHelper.ID_DEPLOYED)
                    .setCode(BundleStatusItemStubHelper.NAME_DEPLOYED),
            new EntandoBundle().setRepoUrl("http://www.myfakebundle3.com")
                    .setCode("strangeBUndle")
    );

    private final BundleStatusHelper bundleStatusHelper = new BundleStatusHelper();

    // required to allow inline list construction with new URL
    BundleStatusHelperTest() throws MalformedURLException {
    }

    /**************************************************************************************************
     * BY URL
     **************************************************************************************************/

    @Test
    void shouldReturnInstalledButNotDeployedStatusItemForBundlesFoundByUrlInTheInstalledButNotDeployedBundleList()
            throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItemByUrl(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed());
    }

    @Test
    void shouldReturnInstalledStatusItemForBundlesFoundByUrlInTheInstalledBundleList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItemByUrl(BundleStatusItemStubHelper.ID_INSTALLED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled());
    }

    @Test
    void shouldReturnDeployedStatusItemForBundlesFoundByUrlInTheDeployedBundleList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItemByUrl(BundleStatusItemStubHelper.ID_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemDeployed());
    }

    @Test
    void shouldReturnNotFoundStatusItemForBundlesByUrlNotPresentInAnyList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItemByUrl(BundleStatusItemStubHelper.ID_NOT_FOUND,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemNotFoundByUrl());
    }

    private void shouldComposeTheExpectedBundleStatusItemByUrl(String repoUrl,
            List<EntandoBundleEntity> installedBundleEntities, List<EntandoBundle> deployedBundles,
            List<EntandoBundleEntity> installedButNotDeployed, BundlesStatusItem expected)
            throws MalformedURLException {

        final BundlesStatusItem actual = bundleStatusHelper.composeBundleStatusItemByURL(new URL(repoUrl),
                installedBundleEntities, deployedBundles, installedButNotDeployed);

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    void shouldThrowExceptionIfNullUrlWhenSearchingByUrl() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByURL(null, installedBundleEntities, deployedBundles,
                        installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalledBundleListWhenSearchingByUrl() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByURL(url, null,
                        deployedBundles, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullDeployedBundleListWhenSearchingByUrl() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByURL(url,
                        installedBundleEntities, null, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalledButNotDeployeddBundleListWhenSearchingByUrl() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByURL(url,
                        installedBundleEntities, deployedBundles, null));
    }

    /**************************************************************************************************
     * BY NAME
     **************************************************************************************************/

    @Test
    void shouldReturnInstalledButNotDeployedStatusItemForBundleFoundByNameInTheInstalledButNotDeployedBundleList() {

        shouldComposeTheExpectedBundleStatusItemByName(BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed());
    }

    @Test
    void shouldReturnInstalledStatusItemForBundlesFoundByNameInTheInstalledBundleList() {

        shouldComposeTheExpectedBundleStatusItemByName(BundleStatusItemStubHelper.NAME_INSTALLED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled());
    }

    @Test
    void shouldReturnDeployedStatusItemForBundlesFoundByNameInTheDeployedBundleList() {

        shouldComposeTheExpectedBundleStatusItemByName(BundleStatusItemStubHelper.NAME_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemDeployed());
    }

    @Test
    void shouldReturnNotFoundStatusItemForBundlesByNameNotPresentInAnyList() {

        shouldComposeTheExpectedBundleStatusItemByName(BundleStatusItemStubHelper.NAME_NOT_FOUND,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemNotFoundByName());
    }

    private void shouldComposeTheExpectedBundleStatusItemByName(String name,
            List<EntandoBundleEntity> installedBundleEntities, List<EntandoBundle> deployedBundles,
            List<EntandoBundleEntity> installedButNotDeployed, BundlesStatusItem expected) {

        final BundlesStatusItem actual = bundleStatusHelper.composeBundleStatusItemByName(name,
                installedBundleEntities, deployedBundles, installedButNotDeployed);

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    void shouldThrowExceptionIfEmptyOrNullNameWhenSearchingByName() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByName(null, installedBundleEntities, deployedBundles,
                        installedButNotDeployed));

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByName("", installedBundleEntities, deployedBundles,
                        installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalledBundleListWhenSearchingByName() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByName(BundleStatusItemStubHelper.NAME_INSTALLED,
                        null, deployedBundles, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullDeployedBundleListWhenSearchingByName() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByName(BundleStatusItemStubHelper.NAME_INSTALLED,
                        installedBundleEntities, null, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalledButNotDeployeddBundleListWhenSearchingByName() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItemByName(BundleStatusItemStubHelper.NAME_INSTALLED,
                        installedBundleEntities, deployedBundles, null));
    }
}
