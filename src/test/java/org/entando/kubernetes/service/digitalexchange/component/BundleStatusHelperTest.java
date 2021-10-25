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
                    .setRepoUrl(new URL(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED))
                    .setVersion(BundleStatusItemStubHelper.INSTALLED_VERSION_INSTALLED_NOT_DEPLOYED),
            new EntandoBundleEntity().setRepoUrl(new URL("http://www.myfakebundle1.com")).setVersion("v1.0.0")
    );
    private final List<EntandoBundleEntity> installedBundleEntities = List.of(
            new EntandoBundleEntity().setRepoUrl(new URL(BundleStatusItemStubHelper.ID_INSTALLED))
                    .setVersion(BundleStatusItemStubHelper.INSTALLED_VERSION_INSTALLED),
            new EntandoBundleEntity().setRepoUrl(new URL("http://www.myfakebundle2.com")).setVersion("v2.0.0")
    );
    private final List<EntandoBundle> deployedBundles = List.of(
            new EntandoBundle().setRepoUrl(BundleStatusItemStubHelper.ID_DEPLOYED),
            new EntandoBundle().setRepoUrl("http://www.myfakebundle3.com")
    );

    private final BundleStatusHelper bundleStatusHelper = new BundleStatusHelper();

    // required to allow inline list construction with new URL
    BundleStatusHelperTest() throws MalformedURLException {
    }

    @Test
    void shouldReturnInstalledButNotDeployedStatusItemForBundlesFoundInTheInstalledButNotDeployedBundleList()
            throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItem(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed());
    }

    @Test
    void shouldReturnInstalledStatusItemForBundlesFoundInTheInstalledBundleList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItem(BundleStatusItemStubHelper.ID_INSTALLED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled());
    }

    @Test
    void shouldReturnDeployedStatusItemForBundlesFoundInTheDeployedBundleList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItem(BundleStatusItemStubHelper.ID_DEPLOYED,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemDeployed());
    }

    @Test
    void shouldReturnNotFoundStatusItemForBundlesNotPresentInAnyList() throws MalformedURLException {

        shouldComposeTheExpectedBundleStatusItem(BundleStatusItemStubHelper.ID_NOT_FOUND,
                installedBundleEntities, deployedBundles, installedButNotDeployed,
                BundleStatusItemStubHelper.stubBundleStatusItemNotFound());
    }

    private void shouldComposeTheExpectedBundleStatusItem(String repoUrl,
            List<EntandoBundleEntity> installedBundleEntities, List<EntandoBundle> deployedBundles,
            List<EntandoBundleEntity> installedButNotDeployed, BundlesStatusItem expected)
            throws MalformedURLException {

        final BundlesStatusItem actual = bundleStatusHelper.composeBundleStatusItem(new URL(repoUrl),
                installedBundleEntities, deployedBundles, installedButNotDeployed);

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    void shouldThrowExceptionIfEmptyOrNullUrl() {

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItem(null, installedBundleEntities, deployedBundles,
                        installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalledBundleList() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItem(url, null,
                        deployedBundles, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullDeployedBundleList() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItem(url,
                        installedBundleEntities, null, installedButNotDeployed));
    }

    @Test
    void shouldThrowExceptionIfNullInstalleButNotDeployeddBundleList() throws MalformedURLException {

        final URL url = BundleStatusItemStubHelper.stubURLInvalidUrl();

        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> bundleStatusHelper.composeBundleStatusItem(url,
                        installedBundleEntities, deployedBundles, null));
    }
}
