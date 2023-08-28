// configure tenant code "primary" in the context holder

package org.entando.kubernetes.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


@Slf4j
public class TenantSecurityKeycloakMockServerJunitExt implements BeforeAllCallback, AfterAllCallback {
    private WireMockServer wireMockServer;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        log.info("Setting Keycloak mock server");
        wireMockServer = EntandoKeycloakMockServer.mockAndStartServer(8899, "entando");

    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        log.info("Cleaning Keycloak mock server");
        EntandoKeycloakMockServer.stopServer(wireMockServer);
    }
}
