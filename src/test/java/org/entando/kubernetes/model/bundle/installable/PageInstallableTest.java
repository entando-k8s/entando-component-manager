package org.entando.kubernetes.model.bundle.installable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.stubhelper.PageStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageInstallableTest {

    @Spy
    private EntandoCoreClientTestDouble entandoCoreClientTestDouble;
    private PageDescriptor pageDescriptor;
    private PageInstallable pageInstallable;

    @BeforeEach
    public void setup() {
        this.pageDescriptor = PageStubHelper.stubPageDescriptor();
    }

    @Test
    void shouldUnpublishAndDeletePageOnUninstallIfConflictStrategyIsCreate() {

        pageInstallable = new PageInstallable(entandoCoreClientTestDouble, pageDescriptor, InstallAction.CREATE);
        pageInstallable.uninstall().join();
        verify(entandoCoreClientTestDouble, times(1)).setPageStatus(PageStubHelper.PAGE_CODE, "draft");
    }

    @Test
    void shouldNOTUnpublishAndNOTDeletePageOnUninstallIfConflictStrategyIsNOTCreate() {

        pageInstallable = new PageInstallable(entandoCoreClientTestDouble, pageDescriptor, InstallAction.OVERRIDE);
        pageInstallable.uninstall().join();
        verify(entandoCoreClientTestDouble, times(0)).setPageStatus(anyString(), anyString());
    }
}
