// configure tenant code "primary" in the context holder

package org.entando.kubernetes.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;


@Slf4j
@UtilityClass
public class TenantTestUtils {

    public static void setPrimaryTenant() {
        log.info("Setting primary tenant code within the context");
        TenantContextHolder.setCurrentTenantCode("primary");
    }

    public void cleanTenantContext() {
        log.info("Cleaning tenant code within the context");
        TenantContextHolder.destroy();
    }

    public static void executeInPrimaryTenantContext(Runnable runnable) {
        setPrimaryTenant();
        runnable.run();
        cleanTenantContext();
    }
}
