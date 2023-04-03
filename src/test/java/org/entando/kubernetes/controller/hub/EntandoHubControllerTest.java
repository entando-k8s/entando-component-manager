package org.entando.kubernetes.controller.hub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.assertionhelper.HubAssertionHelper;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.service.HubService;
import org.entando.kubernetes.stubhelper.HubStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoHubControllerTest {

    private EntandoHubController controller;
    @Mock
    private HubService hubService;

    @BeforeEach
    public void setup() throws Exception {
        try {
            controller = new EntandoHubController(hubService);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testBundleGroupControllerService() {

        when(hubService.searchBundleGroupVersions(anyString(), any()))
                .thenReturn(HubStubHelper.stubBundleGroupVersionsProxiedPayload());

        final PagedContent pagedContent = controller.getBundleGroupVersionsAndFilterThem(
                "registry-123", 1, 1, new String[]{"v1", "v5"});
        HubAssertionHelper.assertOnBundleGroupVersionsPagedContent(pagedContent);
    }

    @Test
    void testBundleGroupControllerServiceInvlalidData() {

        when(hubService.searchBundleGroupVersions(anyString(), any()))
                .thenReturn(HubStubHelper.stubBundleGroupVersionsProxiedPayload());

        final PagedContent pagedContent = controller.getBundleGroupVersionsAndFilterThem(
                "registry-123", null, null, null);
        HubAssertionHelper.assertOnBundleGroupVersionsPagedContent(pagedContent);
    }

    @Test
    void testBundleControllerService() {

        when(hubService.getBundles(anyString(), any()))
                .thenReturn(HubStubHelper.stubBundleDtosProxiedPayload());

        PagedContent<BundleDto, BundleEntityDto> result = controller.getBundles("registry-123", 1, 1, null,
                new String[]{"v1", "v5"});
        HubAssertionHelper.assertOnBundlePagedContent(result);
    }


}
