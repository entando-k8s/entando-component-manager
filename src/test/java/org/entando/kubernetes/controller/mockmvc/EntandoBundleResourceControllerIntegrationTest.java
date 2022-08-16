package org.entando.kubernetes.controller.mockmvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.assertionhelper.BundleAssertionHelper;
import org.entando.kubernetes.assertionhelper.BundleStatusItemAssertionHelper;
import org.entando.kubernetes.assertionhelper.SimpleRestResponseAssertionHelper;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.exception.web.AuthorizationDeniedException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleStatus;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
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
    @MockBean
    private AuthorizationChecker authorizationChecker;

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
        ((K8SServiceClientTestDouble) k8sServiceClient).cleanInMemoryDatabases();
    }

    @Test
    void shouldCorrectlyDeployAnEntandoDeBundle() {

        doNothing().when(authorizationChecker).checkPermissions(anyString());

        Stream.of(
                        BundleInfoStubHelper.GIT_REPO_ADDRESS,
                        "git@www.github.com/entando/mybundle.git",
                        "git://www.github.com/entando/mybundle.git",
                        "ssh://www.github.com/entando/mybundle.git")
                .forEach(bundleUrl -> {
                    try {
                        // given that the user wants to deploy an EntandoDeBundle using a bundleInfo
                        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo()
                                .setGitRepoAddress(bundleUrl);

                        // when the user sends the request
                        String payload = mapper.writeValueAsString(bundleInfo);
                        final ResultActions resultActions = mockMvc.perform(post(componentsUrl)
                                        .header(HttpHeaders.AUTHORIZATION, "jwt")
                                        .content(payload)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk());

                        // then he receives success and the expected deployed EntandoBundle
                        EntandoBundleVersion latestVersion = new EntandoBundleVersion()
                                .setVersion(BundleStubHelper.V1_2_0);
                        EntandoBundle entandoBundle = new EntandoBundle()
                                .setCode("something-77b2b10e")
                                .setTitle(BundleInfoStubHelper.NAME)
                                .setDescription("bundle description")
                                .setRepoUrl(bundleUrl)
                                .setBundleType(BundleType.STANDARD_BUNDLE)
                                .setThumbnail(BundleInfoStubHelper.DESCR_IMAGE)
                                .setComponentTypes(
                                        Set.of("widget", "contentTemplate", "pageTemplate", "language", "label",
                                                "content",
                                                "fragment",
                                                "plugin", "page", "category", "asset", "bundle", "contentType",
                                                "group"))
                                .setLatestVersion(latestVersion);

                        BundleAssertionHelper.assertOnEntandoBundle(resultActions, entandoBundle);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    void shouldReturnErrorWhileDeployingBundleAndReceivingEmptyOrNullRepoUrl() throws Exception {

        // given that the user wants to deploy an EntandoDeBundle using a bundleInfo with an empty repoUrl
        BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo()
                .setGitRepoAddress("");
        // when the user sends the request
        // then he receives 4xx status code
        String payload = mapper.writeValueAsString(bundleInfo);
        mockMvc.perform(post(componentsUrl)
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());

        // given that the user wants to deploy an EntandoDeBundle using a bundleInfo with a null repoUrl
        bundleInfo = BundleInfoStubHelper.stubBunbleInfo()
                .setGitRepoAddress(null);
        // when the user sends the request
        // then he receives 4xx status code
        payload = mapper.writeValueAsString(bundleInfo);
        mockMvc.perform(post(componentsUrl)
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturnErrorWhileDeployingBundleWithoutPermissions() throws Exception {

        doThrow(new AuthorizationDeniedException("err")).when(authorizationChecker).checkPermissions(anyString());

        // given that the user wants to deploy an EntandoDeBundle using a bundleInfo with an empty repoUrl
        BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo()
                .setGitRepoAddress("");
        // when the user sends the request
        // then he receives 4xx status code
        String payload = mapper.writeValueAsString(bundleInfo);
        mockMvc.perform(post(componentsUrl)
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldSuccessfullyUndeployAnExistingEntandoDeBundle() throws Exception {

        doNothing().when(authorizationChecker).checkPermissions(anyString());

        // given an existing bundle in the cluster
        final EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        K8SServiceClientTestDouble kc = (K8SServiceClientTestDouble) k8sServiceClient;
        kc.addInMemoryBundle(bundle);

        // when the user sends the request to undeploy the bundle
        // then he receives 200 status code and the name of the bundle
        mockMvc.perform(delete(componentsUrl + "/" + TestEntitiesGenerator.BUNDLE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.payload.name", is(TestEntitiesGenerator.BUNDLE_NAME)));
    }

    @Test
    void shouldSuccessfullyUndeployANonExistingEntandoDeBundle() throws Exception {

        doNothing().when(authorizationChecker).checkPermissions(anyString());

        // given that no bundles exist in the cluster
        // when the user sends the request to undeploy a bundle
        // then he receives 200 status code and the name of the bundle (even if the bundle does not exist)
        mockMvc.perform(delete(componentsUrl + "/" + TestEntitiesGenerator.BUNDLE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.payload.name", is(TestEntitiesGenerator.BUNDLE_NAME)));
    }

    @Test
    void shouldReturnErrorWhileUndeployingBundleWithoutPermissions() throws Exception {

        doThrow(new AuthorizationDeniedException("err")).when(authorizationChecker).checkPermissions(anyString());

        // given that the user wants to undeploy an EntandoDeBundle
        // when the user sends the request
        // then he receives 4xx status code
        mockMvc.perform(delete(componentsUrl + "/" + TestEntitiesGenerator.BUNDLE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturnEmptyArrayWhenReceivingAnEmptyListOfBundleIds() throws Exception {

        // given that the user asks for the status of an empty list of bundle ids
        String payload = mapper.writeValueAsString(new BundlesStatusQuery());

        // when the user sends the request
        // then he gets an error
        mockMvc.perform(post(componentsUrl + "/status/query")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.bundlesStatuses", hasSize(0)));
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
                new BundlesStatusItem(notFound1, null, BundleStatus.NOT_FOUND, null),
                new BundlesStatusItem(notFound2, null, BundleStatus.NOT_FOUND, null));
        BundleStatusItemAssertionHelper.assertOnBundlesStatusItemList(resultActions, bundlesStatusItemList);
    }

    @Test
    void shouldReceiveTheExpectedBundleStatusItemList() throws Exception {

        // given that some bundles are deployed in the cluster and some bundles are installed in the database
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
                new BundlesStatusItem(TestEntitiesGenerator.BUNDLE_TARBALL_URL, null, BundleStatus.INSTALLED, "v1.2.0"),
                new BundlesStatusItem(deployedRepoUrl, null, BundleStatus.DEPLOYED, null),
                new BundlesStatusItem(installedNotDeployedRepoUrl, null, BundleStatus.INSTALLED_NOT_DEPLOYED, "v1.1.0"),
                new BundlesStatusItem(BundleStatusItemStubHelper.ID_NOT_FOUND, null, BundleStatus.NOT_FOUND, null),
                new BundlesStatusItem(BundleStatusItemStubHelper.ID_INVALID_REPO_URL, null,
                        BundleStatus.INVALID_REPO_URL,
                        null));

        BundleStatusItemAssertionHelper.assertOnBundlesStatusItemList(resultActions, bundlesStatusItemList);
    }

    @Test
    void shouldReceiveTheExpectedBundleStatusItemForAnInstalledBundle() throws Exception {

        BundlesStatusItem expected = new BundlesStatusItem(TestEntitiesGenerator.BUNDLE_TARBALL_URL,
                TestEntitiesGenerator.BUNDLE_NAME, BundleStatus.INSTALLED, "v1.2.0");

        execGetBundleStatusItemByNameTest(TestEntitiesGenerator.BUNDLE_NAME, expected);
    }

    @Test
    void shouldReceiveTheExpectedBundleStatusItemForAnInstalledButNotDeployedBundle() throws Exception {

        BundlesStatusItem expected = new BundlesStatusItem(installedNotDeployedRepoUrl,
                BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED, BundleStatus.INSTALLED_NOT_DEPLOYED, "v1.1.0");

        execGetBundleStatusItemByNameTest(BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED, expected);
    }

    @Test
    void shouldReceiveTheExpectedBundleStatusItemForADeployedBundle() throws Exception {

        BundlesStatusItem expected = new BundlesStatusItem(deployedRepoUrl,
                BundleStatusItemStubHelper.NAME_DEPLOYED, BundleStatus.DEPLOYED, null);

        execGetBundleStatusItemByNameTest(BundleStatusItemStubHelper.NAME_DEPLOYED, expected);
    }

    @Test
    void shouldReceiveTheExpectedBundleStatusItemForANotFoundBundle() throws Exception {

        BundlesStatusItem expected = new BundlesStatusItem(null, BundleStatusItemStubHelper.NAME_NOT_FOUND,
                BundleStatus.NOT_FOUND, null);

        execGetBundleStatusItemByNameTest(BundleStatusItemStubHelper.NAME_NOT_FOUND, expected);
    }

    void execGetBundleStatusItemByNameTest(String bundleName, BundlesStatusItem expected) throws Exception {

        // given that the some bundles are deployed in the cluster and some bundles are installed in the database
        prepareInMemoryBundlesAndDbForBundleStatusTests();

        // when the user requests for the status of one bundle by name
        final ResultActions resultActions = mockMvc.perform(get(componentsUrl + "/status/" + bundleName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk());

        // then he receives the expected result
        BundleStatusItemAssertionHelper.assertOnBundlesStatusItem(resultActions,
                SimpleRestResponseAssertionHelper.BUNDLE_STATUSES_BASE_JSON_PATH, null, expected);
    }


    private void prepareInMemoryBundlesAndDbForBundleStatusTests() {

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
        installedBundleEntity.setRepoUrl(TestEntitiesGenerator.BUNDLE_TARBALL_URL);
        // TODO configure installed bundle in db

        final EntandoDeBundle deployedBundle = TestEntitiesGenerator.getTestBundle();
        deployedBundle.setSpec(TestEntitiesGenerator.getTestEntandoDeBundleSpec(deployedRepoUrl,
                BundleStatusItemStubHelper.NAME_DEPLOYED));
        deployedBundle.getMetadata().setName(BundleStatusItemStubHelper.NAME_DEPLOYED);
        kc.addInMemoryBundle(deployedBundle);

        EntandoBundleEntity installedNotDeployedBundleEntity = TestEntitiesGenerator.getTestComponent()
                .setRepoUrl(installedNotDeployedRepoUrl);
        installedNotDeployedBundleEntity.setBundleCode(BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED);
        installedNotDeployedBundleEntity.setName(BundleStatusItemStubHelper.NAME_INSTALLED_NOT_DEPLOYED);
        installedNotDeployedBundleEntity.setBundleType(BundleType.STANDARD_BUNDLE.getType());
        installedNotDeployedBundleEntity.setVersion("v1.1.0");
        installedNotDeployedBundleEntity.setJob(job);
        installedNotDeployedBundleEntity.setType(Set.of("widget", "plugin", "bundle"));
        installedNotDeployedBundleEntity.setRepoUrl(installedNotDeployedRepoUrl);

        bundleEntityRepository.saveAll(List.of(installedBundleEntity, installedNotDeployedBundleEntity));
    }
}
