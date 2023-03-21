package org.entando.kubernetes.model.bundle.installable;

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
class PageConfigurationInstallableTest {

    @Spy
    private EntandoCoreClientTestDouble entandoCoreClientTestDouble;
    private PageDescriptor pageDescriptor;
    private PageConfigurationInstallable pageConfigurationInstallable;

    @BeforeEach
    void setup() {
        this.pageDescriptor = PageStubHelper.stubPageConfigurationDescriptor();
    }

    @Test
    void shouldSetPageConfigurationAndStatusIfConflictStrategyIsCreate() {
        pageConfigurationInstallable = new PageConfigurationInstallable(entandoCoreClientTestDouble, pageDescriptor, InstallAction.CREATE);
        pageConfigurationInstallable.install().join();
        verify(entandoCoreClientTestDouble, times(1)).updatePageConfiguration(pageDescriptor);
        verify(entandoCoreClientTestDouble, times(1)).setPageStatus(PageStubHelper.PAGE_CODE, PageStubHelper.PAGE_STATUS);
    }

}
