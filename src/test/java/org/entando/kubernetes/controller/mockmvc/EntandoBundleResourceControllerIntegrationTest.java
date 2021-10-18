package org.entando.kubernetes.controller.mockmvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.assertionhelper.BundleStatusItemAssertionHelper;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8100)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
class EntandoBundleResourceControllerIntegrationTest {

    private final String componentsUrl = "/components";

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @Autowired
    private WebApplicationContext context;

    @SpyBean
    private K8SServiceClient k8sServiceClient;
    @Autowired
    private EntandoBundleService bundleService;
    @Autowired
    private InstalledEntandoBundleRepository bundleEntityRepository;
    @Autowired
    private EntandoBundleJobRepository bundleJobRepository;

    final String deployedRepoUrl = "http://www.entando.bundle.git";
    final String installedNotDeployedRepoUrl = "http://www.entando.bundle-not-deployed.git";

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        mapper = new ObjectMapper();
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
        bundleEntityRepository.deleteAll();
        bundleJobRepository.deleteAll();
    }

    @Test
    void shouldCorrectlyDeployAnEntandoDeBundle() throws Exception {

        final EntandoDeBundle deBundle = TestEntitiesGenerator.getTestBundle();
        final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();

        String payload = mapper.writeValueAsString(deBundle);
        mockMvc.perform(post(componentsUrl)
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code", is(deBundle.getMetadata().getName())))
                .andExpect(jsonPath("$.payload.title", is(details.getName())))
                .andExpect(jsonPath("$.payload.description", is(details.getDescription())))
                .andExpect(jsonPath("$.payload.bundleType", is(BundleType.STANDARD_BUNDLE.getType())))
                .andExpect(jsonPath("$.payload.thumbnail", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.componentTypes", hasSize(1)))
                .andExpect(jsonPath("$.payload.componentTypes.[0]", is("bundle")))
                .andExpect(jsonPath("$.payload.installedJob", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.lastJob", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.customInstallation", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.latestVersion.version", is("0.0.15")));
    }

    @Test
    void shouldReturnErrorWhenReceivingAnEmptyListOfBundleIds() throws Exception {

        // given that the user asks for the status of an empty list of bundle ids
        String payload = mapper.writeValueAsString(new BundlesStatusQuery());

        // when the user sends the request
        // then he gets an error
        mockMvc.perform(post(componentsUrl + "/status/query")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", is("Empty list of bundles received")));
    }

    @Test
    void shouldReturnNotFoundForEveryIdNotPresentInTheDbNorDeployedInTheCluster() throws Exception {

        // given that the user asks for the status of a list of bundles ids not present in the system
        String notFound1 = "http://notfound.com";
        String notFound2 = "https://notfoundone.com";

        final BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery().setIds(
                List.of(notFound1, notFound2));

        // when the user sends the request
        String payload = mapper.writeValueAsString(bundlesStatusQuery);
        final ResultActions resultActions = mockMvc.perform(post(componentsUrl + "/status/query")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful());

        // then he receives the expected result
        List<BundlesStatusItem> bundlesStatusItemList = List.of(
                new BundlesStatusItem(notFound1, BundleStatus.NOT_FOUND, null),
                new BundlesStatusItem(notFound2, BundleStatus.NOT_FOUND, null));
        BundleStatusItemAssertionHelper.assertOnBundlesStatusItemList(resultActions, bundlesStatusItemList);
    }

    @Test
    void shouldReturnTheExpectedBundleStatusItemList() throws Exception {

        // given that the some bundles are deployed in the cluster and some bundles are installed in the database
        prepareInMemoryBundlesAndDbForBundleStatusTests();

        // when the user requests for the status of some bundles
        BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery().setIds(List.of(
                TestEntitiesGenerator.BUNDLE_TARBALL_URL, deployedRepoUrl, installedNotDeployedRepoUrl,
                BundleStatusItemStubHelper.ID_NOT_FOUND, BundleStatusItemStubHelper.ID_INVALID_REPO_URL));

        String payload = mapper.writeValueAsString(bundlesStatusQuery);
        final ResultActions resultActions = mockMvc.perform(post(componentsUrl + "/status/query")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        // then he receives the expected result
        List<BundlesStatusItem> bundlesStatusItemList = List.of(
                new BundlesStatusItem(TestEntitiesGenerator.BUNDLE_TARBALL_URL, BundleStatus.INSTALLED, "v1.2.0"),
                new BundlesStatusItem(deployedRepoUrl, BundleStatus.DEPLOYED, null),
                new BundlesStatusItem(installedNotDeployedRepoUrl, BundleStatus.INSTALLED_NOT_DEPLOYED, "v1.1.0"),
                new BundlesStatusItem(BundleStatusItemStubHelper.ID_NOT_FOUND, BundleStatus.NOT_FOUND, null),
                new BundlesStatusItem(BundleStatusItemStubHelper.ID_INVALID_REPO_URL, BundleStatus.INVALID_REPO_URL,
                        null));

        BundleStatusItemAssertionHelper.assertOnBundlesStatusItemList(resultActions, bundlesStatusItemList);
    }


    private void prepareInMemoryBundlesAndDbForBundleStatusTests() throws MalformedURLException {

        final EntandoBundleJobEntity job = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED);
        bundleJobRepository.save(job);

        K8SServiceClientTestDouble kc = (K8SServiceClientTestDouble) k8sServiceClient;

        final EntandoDeBundle installedBundle = TestEntitiesGenerator.getTestBundle();
        kc.addInMemoryBundle(installedBundle);
        final EntandoBundleEntity installedBundleEntity = bundleService.convertToEntityFromEcr(installedBundle);
        installedBundleEntity.setBundleType(BundleType.STANDARD_BUNDLE.getType());
        installedBundleEntity.setVersion("v1.2.0");
        installedBundleEntity.setJob(job);
        installedBundleEntity.setType(Set.of("widget", "plugin", "bundle"));
        installedBundleEntity.setRepoUrl(new URL(TestEntitiesGenerator.BUNDLE_TARBALL_URL));
        // TODO configure installed bundle in db

        final EntandoDeBundle deployedBundle = TestEntitiesGenerator.getTestBundle();
        deployedBundle.setSpec(TestEntitiesGenerator.getTestEntandoDeBundleSpec(deployedRepoUrl));
        kc.addInMemoryBundle(deployedBundle);

        EntandoBundleEntity installedNotDeployedBundleEntity = TestEntitiesGenerator.getTestComponent()
                .setRepoUrl(new URL(installedNotDeployedRepoUrl));
        installedNotDeployedBundleEntity.setId("inst_not_dep");
        installedNotDeployedBundleEntity.setBundleType(BundleType.STANDARD_BUNDLE.getType());
        installedNotDeployedBundleEntity.setVersion("v1.1.0");
        installedNotDeployedBundleEntity.setJob(job);
        installedNotDeployedBundleEntity.setType(Set.of("widget", "plugin", "bundle"));
        installedNotDeployedBundleEntity.setRepoUrl(new URL(installedNotDeployedRepoUrl));

        bundleEntityRepository.saveAll(List.of(installedBundleEntity, installedNotDeployedBundleEntity));
    }
}
