package org.entando.kubernetes.config.tenant.thread;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    public static String getCurrentTenantCode() {
        log.debug("Getting tenant {}", threadLocal.get().getTenantCode());
        return threadLocal.get().getTenantCode();
    }

    public static void setCurrentTenantCode(String tenant) {
        log.debug("Setting tenant {}", tenant);
        threadLocal.set(new TenantContext(tenant, getNullableVirtualContext()));
    }

    public static String getCurrentVirtualContext() {
        String res = getNullableVirtualContext();
        log.debug("Getting virtual context {}", res);
        return res;
    }

    public static void setCurrentVirtualContext(String context) {
        log.debug("Setting virtual context {}", context);
        threadLocal.set(new TenantContext(getNullableTenantCode(), context));
    }

    private static final ThreadLocal<TenantContext> threadLocal = new InheritableThreadLocal<>() {

        @Override
        protected TenantContext childValue(TenantContext parentValue) {
            return new TenantContext(parentValue.getTenantCode(), parentValue.getVirtualContext());
        }
    };

    public static void destroy() {
        log.debug("destroy tenant context");
        threadLocal.remove();
    }

    private static String getNullableVirtualContext() {
        return (threadLocal.get() == null) ? null : threadLocal.get().getVirtualContext();
    }

    private static String getNullableTenantCode() {
        return (threadLocal.get() == null) ? null : threadLocal.get().getTenantCode();
    }
}