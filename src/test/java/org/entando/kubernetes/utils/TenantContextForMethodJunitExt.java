// configure tenant code "primary" in the context holder

package org.entando.kubernetes.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


@Slf4j
public class TenantContextForMethodJunitExt implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        TenantTestUtils.setPrimaryTenant();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TenantTestUtils.cleanTenantContext();
    }
}
