package org.entando.kubernetes.model.bundle.installable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.entando.kubernetes.client.ComponentDataRepositoryTestDouble;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WidgetInstallableTest {

    @Spy
    private EntandoCoreClientTestDouble entandoCoreClientTestDouble;
    @Spy
    private ComponentDataRepositoryTestDouble componentDataRepository;
    private WidgetDescriptor widgetDescriptor;
    private WidgetInstallable widgetInstallable;

    @BeforeEach
    public void setup() {
        this.widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5();
    }

    @Test
    void shouldSkip_install() {
        MockWidgetDescriptor mockDescriptor = new MockWidgetDescriptor();
        mockDescriptor.setDescriptorMetadata(WidgetStubHelper.stubDescriptorMetadata());
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, mockDescriptor,
                InstallAction.SKIP,
                componentDataRepository);
        verify(entandoCoreClientTestDouble, times(0)).createWidget(any());
        verify(entandoCoreClientTestDouble, times(0)).updateWidget(any());
        verify(componentDataRepository, times(0)).save(any());

    }


    @Test
    void shouldInstall_StrategyIsCreate() {
        componentDataRepository = new ComponentDataRepositoryTestDouble();
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();
        verify(entandoCoreClientTestDouble, times(1)).createWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(1);
        assertThat(componentDataRepository.findAll().get(0).getComponentName()).isEqualTo(widgetDescriptor.getName());
    }

    @Test
    void shouldInstall_StrategyIsCreate_WidgetV1WithoutNameButCode() {

        this.widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1()
                .setType(WidgetDescriptor.TYPE_WIDGET_APPBUILDER);
        componentDataRepository = new ComponentDataRepositoryTestDouble();
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();
        verify(entandoCoreClientTestDouble, times(1)).createWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(1);
        assertThat(componentDataRepository.findAll().get(0).getComponentCode()).isEqualTo(WidgetStubHelper.WIDGET_1_CODE);
        assertThat(componentDataRepository.findAll().get(0).getComponentName()).isEqualTo(WidgetStubHelper.WIDGET_1_CODE);
    }

    @Test
    void shouldInstallAndUpdate_StrategyIsCreate() {
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();

        verify(entandoCoreClientTestDouble, times(1)).createWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(1);
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.OVERRIDE,
                componentDataRepository);
        widgetInstallable.install().join();

        verify(entandoCoreClientTestDouble, times(1)).updateWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(1);

    }

    @Test
    void shouldUninstall_StrategyIsCreate() {
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();

        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.uninstallFromEcr().join();
        //verify(entandoCoreClientTestDouble, times(1)).deleteWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(0);
    }


    @Test
    void shouldNOTUnpublishAndNOTDeleteIfConflictStrategyIsNOTCreate() {
        // install just one
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();

        // try to delete just one
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.OVERRIDE,
                componentDataRepository);
        widgetInstallable.uninstallFromEcr().join();
        verify(entandoCoreClientTestDouble, times(0)).deleteWidget(anyString());
        assertThat(componentDataRepository.count()).isEqualTo(0);


    }

    public static class MockWidgetDescriptor extends WidgetDescriptor {

        private String uncheckedFiled;

        public String getUncheckedFiled() throws IOException {
            throw new IOException();
        }

    }
}
