package org.entando.kubernetes.model.bundle.installable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DirectoryInstallableTest {


    @ParameterizedTest
    @MethodSource("provideValuesThatPreventUninstallation")
    void shouldNotUninstallFromAppEngine(boolean isRoot, InstallAction action, String dirPath) throws ExecutionException, InterruptedException {
        // --GIVEN
        DirectoryInstallable directoryInstallable = new DirectoryInstallable(new DirectoryDescriptor(dirPath, isRoot),
                action);
        // --WHEN
        boolean shouldUninstallFromAppEngine = directoryInstallable.shouldUninstallFromAppEngine();
        // --THEN
        assertFalse(shouldUninstallFromAppEngine);

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
    void shouldUninstallFromAppEngine(boolean isRoot, InstallAction action, String dirPath) throws ExecutionException, InterruptedException {
        // --GIVEN
        DirectoryInstallable directoryInstallable = new DirectoryInstallable(new DirectoryDescriptor(dirPath, isRoot),
                action);
        // --WHEN
        boolean shouldUninstallFromAppEngine = directoryInstallable.shouldUninstallFromAppEngine();
        // --THEN
        assertTrue(shouldUninstallFromAppEngine);

    }

    private static Stream<Arguments> provideValuesThatAllowUninstallation() {
        return Stream.of(
                Arguments.of(true, InstallAction.CREATE, "bundles/anything"),
                Arguments.of(true, InstallAction.CREATE, "anything")

        );
    }
}
