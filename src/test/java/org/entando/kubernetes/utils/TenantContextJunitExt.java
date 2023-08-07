// configure tenant code "primary" in the context holder

package org.entando.kubernetes.utils;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


@Slf4j
public class TenantContextJunitExt implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        log.info("Setting tenant code within the context");
        TenantContextHolder.setCurrentTenantCode("primary");
    }
}
