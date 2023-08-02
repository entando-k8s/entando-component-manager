package org.entando.kubernetes.config.tenant;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    public static String getCurrentTenantCode() {
        System.out.println("################## ");
        log.error("GETTING TENANT {}", threadLocal.get().getTenantCode());
        return threadLocal.get().getTenantCode();
    }
    public static void setCurrentTenantCode(String tenant) {
        System.out.println("################## ");
        log.error("SETTING TENANT {}", tenant);
        threadLocal.set(new TenantContext(tenant));
    }

    private static final ThreadLocal<TenantContext> threadLocal = new InheritableThreadLocal<>() {

        @Override
        protected TenantContext childValue(TenantContext parentValue) {

            final String tenantCode = parentValue.getTenantCode();
            System.out.println("################## ");
            log.error("SETTING CHILD VALUE {}", tenantCode);

            if (parentValue == null) {
                return null;
            }

            return new TenantContext(tenantCode);
        }
    };

    public static void destroy() {
        log.debug("destroy tenant context");
        threadLocal.remove();
    }
}
