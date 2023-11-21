package org.entando.kubernetes.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class TenantSecondaryContextJunitExt implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        TenantTestUtils.setSecondaryTenant();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        TenantTestUtils.cleanTenantContext();
    }

}
