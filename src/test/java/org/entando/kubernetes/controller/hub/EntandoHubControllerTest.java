package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.client.hub.HubClientService;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    public void testMe() {
        PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto> a =
                controller.getaBundleGroupVersionsAndFilterThem("http://localhost:7762", 1, 1, new String[] {"v1", "v5"});
        System.out.println(">>> " + a);

    }

    @BeforeEach
    public void setup() throws Exception {
        try {
            clientService = new HubClientService();
            controller = new EntandoHubController(clientService);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
