package org.entando.kubernetes.service.digitalexchange.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TimedBundleOperationsConcurrencyManagerTest {

    private TimedBundleOperationsConcurrencyManager timedBundleOperationsConcurrencyManager;

    @BeforeEach
    public void setup() {
        timedBundleOperationsConcurrencyManager = new TimedBundleOperationsConcurrencyManager();
    }

    @Test
    void shouldReturnTrueWhenLastOperationStartTimeValueIsNull() {

        // lastOperationStartTime is null by default after the object creation

        boolean result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenLastOperationStartTimeValueIsNOTNullAndIsAfter30MinutesFromNow() {

        boolean result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();

        result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenLastOperationStartTimeValueIsNOTNullAndIsAfter30MinutesFromNow() {

        timedBundleOperationsConcurrencyManager = new TimedBundleOperationsConcurrencyManager(LocalDateTime.now().minusMinutes(31));
        boolean result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();

        timedBundleOperationsConcurrencyManager = new TimedBundleOperationsConcurrencyManager(
                LocalDateTime.now().minusMinutes(60));
        result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();

        timedBundleOperationsConcurrencyManager = new TimedBundleOperationsConcurrencyManager(
                LocalDateTime.now().minusHours(2));
        result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();
    }

    @Test
    void shouldAdmitNewOperationOnlyWhenOperationTerminatedIsInvoked() {

        // last operation started 2 mins ago and can't start a new one
        timedBundleOperationsConcurrencyManager = new TimedBundleOperationsConcurrencyManager(
                LocalDateTime.now().minusMinutes(2));
        boolean result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isFalse();

        // terminate prev operation and can start a new one
        timedBundleOperationsConcurrencyManager.operationTerminated();
        result = timedBundleOperationsConcurrencyManager.manageStartOperation();
        assertThat(result).isTrue();
    }
}