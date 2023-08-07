package org.entando.kubernetes.config.tenant.thread;

import lombok.Getter;

@Getter
public class CurrentRequestThreadState {

    private String tenantCode = null;

    private CurrentRequestThreadState() {
    }

    private CurrentRequestThreadState(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public static CurrentRequestThreadState currentRequestThreadState() {
        return new CurrentRequestThreadState(TenantContextHolder.getCurrentTenantCode());
    }

    public static void setCurrentThreadState(CurrentRequestThreadState currentThreadState) {
        TenantContextHolder.setCurrentTenantCode(currentThreadState.getTenantCode());
    }

    public static void clearCurrentThreadState() {
        TenantContextHolder.destroy();
    }
}