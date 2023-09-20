package org.entando.kubernetes.model.bundle.installable;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DirectoryInstallableTest {

    @Mock
    private EntandoCoreClient engineService;

    @ParameterizedTest
    @MethodSource("provideValuesThatPreventUninstallation")
    void shouldNotUninstall(boolean isRoot, InstallAction action, String dirPath) {
        // --GIVEN
        DirectoryInstallable directoryInstallable = new DirectoryInstallable(engineService, new DirectoryDescriptor(dirPath, isRoot),
                action);
        // --WHEN
        directoryInstallable.uninstall();
        // --THEN
        verify(engineService, times(0)).deleteFolder(dirPath);

    }

    private static Stream<Arguments> provideValuesThatPreventUninstallation() {
        return Stream.of(
                Arguments.of(true, InstallAction.CREATE, "bundles"),
                Arguments.of(false, InstallAction.CREATE, "bundles"),
                Arguments.of(false, InstallAction.CREATE, "bundles/anything"),
                Arguments.of(false, InstallAction.OVERRIDE, "bundles/anything"),
                Arguments.of(true, InstallAction.OVERRIDE, "bundles/anything")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValuesThatAllowUninstallation")
    void shouldUninstall(boolean isRoot, InstallAction action, String dirPath) {
        // --GIVEN
        DirectoryInstallable directoryInstallable = new DirectoryInstallable(engineService, new DirectoryDescriptor(dirPath, isRoot),
                action);
        // --WHEN
        directoryInstallable.uninstall();
        // --THEN
        verify(engineService, times(1)).deleteFolder(dirPath);

    }

    private static Stream<Arguments> provideValuesThatAllowUninstallation() {
        return Stream.of(
                Arguments.of(true, InstallAction.CREATE, "bundles/anything"),
                Arguments.of(true, InstallAction.CREATE, "anything")

        );
    }
}
