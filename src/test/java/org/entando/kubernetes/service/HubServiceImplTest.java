package org.entando.kubernetes.service;

import org.entando.kubernetes.assertionhelper.HubAssertionHelper;
import org.entando.kubernetes.client.HubClientTestDouble;
import org.entando.kubernetes.client.hub.HubClient;
import org.entando.kubernetes.client.hub.ProxiedPayload;
import org.entando.kubernetes.exception.web.NotFoundException;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class HubServiceImplTest {

    private HubServiceImpl hubService;
    private final HubClient hubClient = new HubClientTestDouble();
    @Mock
    private EntandoHubRegistryService registryService;

    @BeforeEach
    public void setUp() {
        hubService = new HubServiceImpl(hubClient, registryService);
    }

    @Test
    void shouldReturnSomeBundleGroupVersionsWhenTheClientSucceeds() {
        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        final ProxiedPayload proxiedPayload = hubService.searchBundleGroupVersions(
                "hostId", Collections.emptyMap());
        HubAssertionHelper.assertOnSuccessfulProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldReturnErrorWhileAskingForBundleGroupVersionsButTheClientFails() {
        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        ((HubClientTestDouble)hubClient).setMustFail(true);

        final ProxiedPayload proxiedPayload = hubService.searchBundleGroupVersions(
                "hostId", Collections.emptyMap());
        HubAssertionHelper.assertOnFailingProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldThrowExceptionWhileAskingForBundleGroupVersionsButTheRegistryServiceFails() {
        when(registryService.getRegistry(anyString())).thenThrow(new NotFoundException("not found"));

        assertThrows(NotFoundException.class, () -> hubService.searchBundleGroupVersions(
                "hostId", Collections.emptyMap()));
    }

    @Test
    void shouldReturnSomeBundleDtoWhenTheClientSucceeds() {
        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        final ProxiedPayload proxiedPayload = hubService.getBundles("hostId", Collections.emptyMap());

        HubAssertionHelper.assertOnSuccessfulProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldReturnErrorWhileAskingForBundleDtosButTheClientFails() {

        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        ((HubClientTestDouble)hubClient).setMustFail(true);
        final ProxiedPayload proxiedPayload = hubService.getBundles("hostId", Collections.emptyMap());

        HubAssertionHelper.assertOnFailingProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldThrowExceptionWhileAskingForBundlesButTheRegistryServiceFails() {
        when(registryService.getRegistry(anyString())).thenThrow(new NotFoundException("not found"));
        assertThrows(NotFoundException.class, () -> hubService.getBundles("hostId", Collections.emptyMap()));
    }

}
