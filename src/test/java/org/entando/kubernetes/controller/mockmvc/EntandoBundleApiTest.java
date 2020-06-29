package org.entando.kubernetes.controller.mockmvc;

import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class EntandoBundleApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Value("${entando.digital-exchanges.names}")
    private List<String> digitalExchangesNames;

    @SpyBean
    private K8SServiceClient k8sServiceClient;

    @BeforeEach
    public void setup() {
        ((K8SServiceClientTestDouble) k8sServiceClient).cleanInMemoryDatabases();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void shouldStart() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    public void apiShouldMaintainCompatibilityWithAppBuilder() throws Exception {

        K8SServiceClientTestDouble kc = (K8SServiceClientTestDouble) k8sServiceClient;
        kc.addInMemoryBundle(getTestBundle());

        mockMvc.perform(get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0]").isMap())
                .andExpect(jsonPath("payload[0]", hasKey("id")))
                .andExpect(jsonPath("payload[0]", hasKey("name")))
                .andExpect(jsonPath("payload[0]", hasKey("lastUpdate")))
                .andExpect(jsonPath("payload[0]", hasKey("version")))
                .andExpect(jsonPath("payload[0]", hasKey("type")))
                .andExpect(jsonPath("payload[0]", hasKey("description")))
                .andExpect(jsonPath("payload[0]", hasKey("image")))
                .andExpect(jsonPath("payload[0]", hasKey("rating")))
                .andExpect(jsonPath("metaData.page").value(1));

        verify(k8sServiceClient, times(1)).getBundlesInObservedNamespaces();
    }

    @Test
    public void apiShouldSupportFiltering() throws Exception {

        K8SServiceClientTestDouble kc = (K8SServiceClientTestDouble) k8sServiceClient;
        kc.addInMemoryBundle(getTestBundle());

        mockMvc.perform(get("/components?filters[0].attribute=type&filters[0].operator=eq&filters[0].value=widget").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0]").isMap())
                .andExpect(jsonPath("payload[0]", hasKey("id")))
                .andExpect(jsonPath("payload[0]", hasKey("name")))
                .andExpect(jsonPath("payload[0]", hasKey("lastUpdate")))
                .andExpect(jsonPath("payload[0]", hasKey("version")))
                .andExpect(jsonPath("payload[0]", hasKey("type")))
                .andExpect(jsonPath("payload[0]", hasKey("description")))
                .andExpect(jsonPath("payload[0]", hasKey("image")))
                .andExpect(jsonPath("payload[0]", hasKey("rating")))
                .andExpect(jsonPath("metaData.page").value(1));

        verify(k8sServiceClient, times(1)).getBundlesInObservedNamespaces();

        mockMvc.perform(get("/components?filters[0].attribute=type&filters[0].operator=eq&filters[0].value=page").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));

    }

    @Test
    public void shouldNotBeAbleToGetComponentsFromNotRegisteredDigitalExchanges() throws Exception {
        K8SServiceClientTestDouble kc = (K8SServiceClientTestDouble) k8sServiceClient;
        EntandoDeBundle bundle = getTestBundle();
        bundle.getMetadata().setNamespace("my-custom-namespace");
        kc.addInMemoryBundle(bundle);

        mockMvc.perform(get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));

        verify(k8sServiceClient, times(1)).getBundlesInObservedNamespaces();

    }

    @Test
    public void shouldReturnBadRequestForNotInstalledBundles() throws Exception {
        mockMvc.perform(get("/components/temp/usage").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundle")
                .withNamespace("entando-de-bundles")
                .addToLabels("widget", "true")
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    private EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("inail_bundle")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8081/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }


}
