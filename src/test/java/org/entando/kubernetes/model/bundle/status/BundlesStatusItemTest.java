package org.entando.kubernetes.model.bundle.status;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.junit.jupiter.api.Test;

class BundlesStatusItemTest {

    @Test
    void shouldReturnTheFullyPopulatedInstance() {

        BundlesStatusItem item = new BundlesStatusItem()
                .setId(BundleStatusItemStubHelper.ID_INSTALLED)
                .setName(BundleStatusItemStubHelper.NAME_INSTALLED)
                .setStatus(BundleStatusItemStubHelper.STATUS_INSTALLED)
                .setInstalledVersion(BundleStatusItemStubHelper.INSTALLED_VERSION_INSTALLED);

        assertThat(item).isEqualToComparingFieldByField(BundleStatusItemStubHelper.stubBundleStatusItem());
    }
}
