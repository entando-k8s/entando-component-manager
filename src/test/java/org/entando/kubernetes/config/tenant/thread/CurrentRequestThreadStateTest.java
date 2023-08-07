package org.entando.kubernetes.config.tenant.thread;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class CurrentRequestThreadStateTest {

    @Test
    void shouldReturnACurrentRequestThreadStateBasedOnCurrentTenantCode() {
        String expected = "primary";
        TenantContextHolder.setCurrentTenantCode(expected);
        final CurrentRequestThreadState currentRequestThreadState = CurrentRequestThreadState.currentRequestThreadState();
        assertThat(currentRequestThreadState.getTenantCode()).isEqualTo(expected);
    }

    @Test
    void shouldUpdateTheCurrentRequestThreadState() {
        String expected = "primary";
        TenantContextHolder.setCurrentTenantCode(expected);
        CurrentRequestThreadState.clearCurrentThreadState();
        assertThrows(NullPointerException.class, CurrentRequestThreadState::currentRequestThreadState);
    }
}