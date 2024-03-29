package org.entando.kubernetes.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
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

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class HubServiceImplTest {

    private final HubClient hubClient = new HubClientTestDouble();
    private HubServiceImpl hubService;
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
        ((HubClientTestDouble) hubClient).setMustFail(true);

        final ProxiedPayload proxiedPayload = hubService.searchBundleGroupVersions(
                "hostId", Collections.emptyMap());
        HubAssertionHelper.assertOnFailingProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldThrowExceptionWhileAskingForBundleGroupVersionsButTheRegistryServiceFails() {
        when(registryService.getRegistry(anyString())).thenThrow(new NotFoundException("not found"));

        final Map<String, Object> emptyMap = Collections.emptyMap();
        assertThrows(NotFoundException.class, () -> hubService.searchBundleGroupVersions(
                "hostId", emptyMap));
    }

    @Test
    void shouldReturnSomeBundleDtoWhenTheClientSucceeds() {
        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        final Map<String, Object> emptyMap = Collections.emptyMap();
        final ProxiedPayload proxiedPayload = hubService.getBundles("hostId", emptyMap);

        HubAssertionHelper.assertOnSuccessfulProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldReturnErrorWhileAskingForBundleDtosButTheClientFails() {

        when(registryService.getRegistry(anyString())).thenReturn(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        ((HubClientTestDouble) hubClient).setMustFail(true);
        final ProxiedPayload proxiedPayload = hubService.getBundles("hostId", Collections.emptyMap());

        HubAssertionHelper.assertOnFailingProxiedPayload(proxiedPayload);
    }

    @Test
    void shouldThrowExceptionWhileAskingForBundlesButTheRegistryServiceFails() {
        when(registryService.getRegistry(anyString())).thenThrow(new NotFoundException("not found"));
        final Map<String, Object> params = Collections.emptyMap();
        assertThrows(NotFoundException.class, () -> hubService.getBundles("hostId", params));
    }

}
