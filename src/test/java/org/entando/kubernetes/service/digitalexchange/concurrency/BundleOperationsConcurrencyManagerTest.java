package org.entando.kubernetes.service.digitalexchange.concurrency;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.exception.digitalexchange.BundleOperationConcurrencyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BundleOperationsConcurrencyManagerTest {

    @Mock
    private BundleOperationsConcurrencyManager bundleOperationsConcurrencyManager;

    @Test
    void shouldDoNothingIfNoPreviousOperationIsRunningInTheAdmittedTimeInterval() {

        when(bundleOperationsConcurrencyManager.manageStartOperation()).thenReturn(true);
        doCallRealMethod().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunning();

        assertAll(() -> bundleOperationsConcurrencyManager.throwIfAnotherOperationIsRunning());
    }

    @Test
    void shouldThrowExceptionIfThereIsAPreviousOperationRunningInTheAdmittedTimeInterval() {

        when(bundleOperationsConcurrencyManager.manageStartOperation()).thenReturn(false);
        doCallRealMethod().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunning();

        Assertions.assertThrows(BundleOperationConcurrencyException.class,
                () -> bundleOperationsConcurrencyManager.throwIfAnotherOperationIsRunning());
    }
}