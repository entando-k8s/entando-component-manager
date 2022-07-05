package org.entando.kubernetes.model.bundle.installable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import org.entando.kubernetes.client.ComponentDataRepositoryTestDouble;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WidgetInstallableTest {

    @Spy
    private EntandoCoreClientTestDouble entandoCoreClientTestDouble;
    @Spy
    private ComponentDataRepositoryTestDouble componentDataRepository;
    private WidgetDescriptor widgetDescriptor;
    private WidgetInstallable widgetInstallable;
    @Mock
    private ObjectMapper jsonMapper;

    @BeforeEach
    public void setup() {
        this.widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5();
        //this.componentDataRepository = new ComponentDataRepositoryTestDouble();
    }

    @Test
    void shouldThrowError_installNull() {
        //componentDataRepository = new ComponentDataRepositoryTestDouble();
        MockWidgetDescriptor mockDescriptor = new MockWidgetDescriptor();
        mockDescriptor.setType(WidgetDescriptor.TYPE_WIDGET_APPBUILDER);
        mockDescriptor.setDescriptorMetadata(WidgetStubHelper.stubDescriptorMetadata());
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, mockDescriptor,
                InstallAction.CREATE,
                componentDataRepository);
        assertThrows(DefaultProblem.class, () -> {
            try {
                widgetInstallable.install().join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
            ;
        });

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
    }

    @Test
    void shouldInstallAndUpdate_StrategyIsCreate() {
        //componentDataRepository = new ComponentDataRepositoryTestDouble();
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
        //componentDataRepository = new ComponentDataRepositoryTestDouble();
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();

        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.uninstall().join();
        verify(entandoCoreClientTestDouble, times(1)).deleteWidget(any());
        assertThat(componentDataRepository.count()).isEqualTo(0);
    }


    @Test
    void shouldNOTUnpublishAndNOTDeleteIfConflictStrategyIsNOTCreate() {
        // install just one
        //componentDataRepository = new ComponentDataRepositoryTestDouble();
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.CREATE,
                componentDataRepository);
        widgetInstallable.install().join();

        // try to delete just one
        widgetInstallable = new WidgetInstallable(entandoCoreClientTestDouble, widgetDescriptor, InstallAction.OVERRIDE,
                componentDataRepository);
        widgetInstallable.uninstall().join();
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
