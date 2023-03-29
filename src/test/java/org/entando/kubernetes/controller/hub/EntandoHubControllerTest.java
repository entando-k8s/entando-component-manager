package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.client.hub.HubClientService;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLEGROUP_RESPONSE_JSON;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLE_RESPONSE_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@AutoConfigureWireMock(port = 7762)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
public class EntandoHubControllerTest {

    @LocalServerPort
    private int localServerPort;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntandoHubController controller;
    private HubClientService clientService;
    private HubClientService hubClientService;
    private EntandoHubRegistryService registryService;


    @BeforeEach
    public void setup() throws Exception {
        try {
            clientService = new HubClientService();
            controller = new EntandoHubController(clientService, registryService);
            // wiremock stuff
            stubFor(get(urlMatching("/appbuilder/api/bundlegroups/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", HAL_JSON_VALUE)
                            .withBody(BUNDLEGROUP_RESPONSE_JSON)));
            stubFor(get(urlMatching("/appbuilder/api/bundles/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", HAL_JSON_VALUE)
                            .withBody(BUNDLE_RESPONSE_JSON)));

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testBundleGroupControllerService() {
        PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> result =
                controller.getBundleGroupVersionsAndFilterThem("http://localhost:7762", 1, 1, new String[] {"v1", "v5"});
        assertNotNull(result);
        assertThat(result, instanceOf(PagedContent.class));

        PagedContent pc = (PagedContent) result;
        assertNotNull(pc.getMetadata());
        assertNotNull(pc.getPayload());
        assertThat(pc.getPayload(), instanceOf(List.class));

        Object elem = pc.getPayload().get(0);
        assertThat(elem, instanceOf(BundleGroupVersionFilteredResponseView.class));
        BundleGroupVersionFilteredResponseView bgv = (BundleGroupVersionFilteredResponseView) elem;
        assertThat(bgv.getBundleGroupId(), equalTo((long)1));
        assertThat(bgv.getBundleGroupVersionId(), equalTo((long)4));
        assertThat(bgv.getDocumentationUrl(), equalTo("http://docm.me"));
        assertThat(bgv.isPublicCatalog(), equalTo(true));
    }

    @Test
    public void testBundleControllerService() {
        PagedContent<BundleDto, BundleEntityDto> result = controller.getBundles("http://localhost:7762", 1, 1, null, new String[]{"v1", "v5"});
        assertNotNull(result);
        assertThat(result, instanceOf(PagedContent.class));

        PagedContent pc = (PagedContent) result;
        assertNotNull(pc.getMetadata());
        assertNotNull(pc.getPayload());
        assertThat(pc.getPayload(), instanceOf(List.class));

        Object elem = pc.getPayload().get(0);
        assertThat(elem, instanceOf(BundleDto.class));
        BundleDto bgv = (BundleDto) elem;
        assertThat(bgv.getBundleId(), equalTo("13"));
        assertThat(bgv.getName(), equalTo("bundle-uri-1"));
        assertThat(bgv.getDescription(), equalTo("Description default"));
        assertThat(bgv.getGitRepoAddress(), equalTo("https://github.com/account/bundle-uri-1.git"));
    }

}
