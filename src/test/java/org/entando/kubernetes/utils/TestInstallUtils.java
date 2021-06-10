package org.entando.kubernetes.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.entando.kubernetes.model.EntandoDeploymentPhase.SUCCESSFUL;
import static org.entando.kubernetes.utils.SleepStubber.doSleep;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.UniformDistribution;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.model.EntandoCustomResourceStatus;
import org.entando.kubernetes.model.EntandoDeploymentPhase;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.NoUsageComponent;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkSpec;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public class TestInstallUtils {

    public static final UriBuilder INSTALL_PLANS_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc", "installplans");
    public static final UriBuilder ALL_COMPONENTS_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components");
    public static final UriBuilder SINGLE_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc");
    public static final UriBuilder INSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc", "install");
    public static final UriBuilder UNINSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc", "uninstall");
    public static final String JOBS_ENDPOINT = "/jobs";
    private static final Duration MAX_WAITING_TIME_FOR_JOB_STATUS = Duration.ofSeconds(30);
    private static final Duration AWAITILY_DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);

    @SneakyThrows
    public static String simulateSuccessfullyCompletedInstall(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        mockSuccessfullyCompletedInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeSuccessfullyCompletedInstall(mockMvc, result);
    }

    @SneakyThrows
    public static String simulateSuccessfullyCompletedInstallWithInstallPlan(MockMvc mockMvc,
            EntandoCoreClient coreClient, K8SServiceClient k8sServiceClient, String bundleName) {

        mockSuccessfullyCompletedInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(put(INSTALL_PLANS_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeSuccessfullyCompletedInstall(mockMvc, result);
    }

    @SneakyThrows
    public static String simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest(MockMvc mockMvc,
            EntandoCoreClient coreClient, K8SServiceClient k8sServiceClient, String bundleName) {

        mockSuccessfullyCompletedInstall(coreClient, k8sServiceClient, bundleName);

        ObjectMapper objectMapper = new ObjectMapper();

        MvcResult result = mockMvc.perform(
                put(INSTALL_PLANS_ENDPOINT.build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockInstallWithPlansRequestWithActions())))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        return completeSuccessfullyCompletedInstall(mockMvc, result);
    }

    /**
     * perform mock operations required to simulate a successfully bundle installation.
     *
     * @param coreClient the EntandoCoreClient
     * @param k8sServiceClient the K8SServiceClient
     * @param bundleName the name of the bundle
     */
    private static void mockSuccessfullyCompletedInstall(EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8sServiceClient);
        mockPlugins(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(bundleName))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
    }

    private static String completeSuccessfullyCompletedInstall(MockMvc mockMvc, MvcResult result) throws Exception {

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_COMPLETED);

        return jobId;
    }

    @SneakyThrows
    public static String simulateSuccessfullyCompletedUpdate(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8sServiceClient);
        mockPlugins(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(bundleName))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        mockAnalysisReport(coreClient, k8sServiceClient);

        InstallRequest request = InstallRequest.builder()
                .conflictStrategy(InstallAction.OVERRIDE)
                .build();

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_COMPLETED);

        return jobId;
    }

    public static EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("todomvc")
                .withNamespace("entando-de-bundles")
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    public static EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("todomvc")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1-alpha")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8099/repository/npm-internal/test_bundle/-/test_bundle-0.0.1-alpha.tgz")
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8099/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }

    public static AnalysisReport getCoreAnalysisReport() {
        return AnalysisReport.builder()
                .categories(Map.of("my-category", Status.NEW, "another_category", Status.DIFF))
                .groups(Map.of("ecr", Status.NEW, "ps", Status.DIFF))
                .labels(Map.of("HELLO", Status.DIFF, "WORLD", Status.NEW))
                .languages(Map.of("en", Status.NEW, "it", Status.DIFF))
                .fragments(Map.of("title_fragment", Status.NEW, "another_fragment", Status.DIFF))
                .pageTemplates(Map.of("todomvc_page_model", Status.NEW, "todomvc_another_page_model", Status.DIFF))
                .pages(Map.of("my-page", Status.NEW, "another-page", Status.DIFF))
                .resources(Map.of("/something/css/custom.css", Status.DIFF, "/something/css/style.css", Status.NEW,
                        "/something/js/configUiScript.js", Status.NEW, "/something/js/script.js", Status.NEW,
                        "/something/js/vendor/jquery/jquery.js", Status.NEW))
                .widgets(Map.of("another_todomvc_widget", Status.DIFF, "todomvc_widget", Status.NEW,
                        "widget_with_config_ui", Status.NEW))
                .build();
    }

    public static InstallPlan mockInstallPlan() {
        return InstallPlan.builder()
                .hasConflicts(true)
                .categories(Map.of("my-category", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "another_category", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .groups(Map.of("ecr", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW), "ps",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .labels(Map.of("HELLO", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF), "WORLD",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .languages(Map.of("en", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW), "it",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .fragments(Map.of("title_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "another_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .pageTemplates(Map.of("todomvc_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "todomvc_another_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .pages(Map.of("my-page", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW), "another-page",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .resources(
                        Map.of("/something/css/custom.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF),
                                "/something/css/style.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                                "/something/js/configUiScript.js",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW), "/something/js/script.js",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                                "/something/js/vendor/jquery/jquery.js",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .widgets(Map.of("another_todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF),
                        "todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "widget_with_config_ui", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .assets(Map.of("my-asset", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "anotherAsset", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF)))
                .contentTypes(
                        Map.of("CNG", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF), "CNT",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .contentTemplates(
                        Map.of("8880002", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF),
                                "8880003", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .contents(Map.of("CNG102", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF),
                        "CNT103", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .plugins(Map.of("custombasename", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF),
                        "entando-todomvcv1-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW),
                        "entando-todomvcv2-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW)))
                .build();
    }


    public static InstallPlan mockInstallPlanWithActions() {
        return InstallPlan.builder()
                .hasConflicts(true)
                .categories(Map.of("my-category", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "another_category", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP)))
                .groups(Map.of("ecr", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "ps",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .labels(Map.of("HELLO", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP), "WORLD",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .languages(Map.of("en", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "it",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .fragments(Map.of("title_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "another_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .pageTemplates(Map.of("todomvc_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "todomvc_another_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP)))
                .pages(Map.of("my-page", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "another-page",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .resources(
                        Map.of("/something/css/custom.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP),
                                "/something/css/style.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                                "/something/js/configUiScript.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                                "/something/js/script.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                                "/something/vendor/jquery/jquery.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.EQUAL, InstallAction.OVERRIDE)))
                .widgets(Map.of("another_todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .assets(Map.of("my-asset", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "anotherAsset", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .contentTypes(
                        Map.of("CNG", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE), "CNT",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .contentTemplates(
                        Map.of("8880002", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP),
                                "8880003", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .contents(Map.of("CNG102", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "CNT103", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .plugins(Map.of("custombasename", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "entando-todomvcv1-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "entando-todomvcv2-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .build();
    }

    public static InstallWithPlansRequest mockInstallWithPlansRequestWithActions() {
        return (InstallWithPlansRequest) new InstallWithPlansRequest()
                .setVersion("0.0.1")
                .setHasConflicts(true)
                .setCategories(Map.of("my-category", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "another_category", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP)))
                .setGroups(Map.of("ecr", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "ps",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .setLabels(Map.of("HELLO", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP), "WORLD",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .setLanguages(Map.of("en", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "it",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .setFragments(Map.of("title_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "another_fragment", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .setPageTemplates(Map.of("todomvc_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "todomvc_another_page_model", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP)))
                .setPages(Map.of("my-page", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE), "another-page",
                        InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .setResources(
                        Map.of("/something/css/custom.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP),
                                "/something/css/style.css", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                                "/something/js/configUiScript.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                                "/something/js/script.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                                "/something/vendor/jquery/jquery.js", InstallPlanStubHelper.stubComponentInstallPlan(Status.EQUAL, InstallAction.OVERRIDE)))
                .setWidgets(Map.of("another_todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "todomvc_widget", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .setAssets(Map.of("my-asset", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "anotherAsset", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE)))
                .setContentTypes(
                        Map.of("CNG", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE), "CNT",
                                InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .setContentTemplates(
                        Map.of("8880002", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.SKIP),
                                "8880003", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .setContents(Map.of("CNG102", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "CNT103", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)))
                .setPlugins(Map.of("custombasename", InstallPlanStubHelper.stubComponentInstallPlan(Status.DIFF, InstallAction.OVERRIDE),
                        "entando-todomvcv1-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE),
                        "entando-todomvcv2-1-0-0", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW, InstallAction.CREATE)));
    }


    public static AnalysisReport getCmsAnalysisReport() {
        return AnalysisReport.builder()
                .assets(Map.of("my-asset", Status.NEW,
                        "anotherAsset", Status.DIFF))
                .contentTypes(
                        Map.of("CNG", Status.DIFF, "CNT",
                                Status.NEW))
                .contentTemplates(
                        Map.of("8880002", Status.DIFF,
                                "8880003", Status.NEW))
                .contents(Map.of("CNG102", Status.DIFF,
                        "CNT103", Status.NEW))
                .build();
    }


    public static AnalysisReport getPluginAnalysisReport() {
        return AnalysisReport.builder()
                .plugins(Map.of("custombasename",
                        Status.DIFF,
                        "entando-todomvcv1-1-0-0",
                        Status.NEW,
                        "entando-todomvcv2-1-0-0",
                        Status.NEW))
                .build();
    }

    public static void mockAnalysisReport(EntandoCoreClient coreClient, K8SServiceClient k8SServiceClient) {
        AnalysisReport coreAnalysisReport = getCoreAnalysisReport();
        AnalysisReport cmsAnalysisReport = getCmsAnalysisReport();
        AnalysisReport pluginAnalysisReport = getPluginAnalysisReport();

        when(coreClient.getEngineAnalysisReport(any())).thenReturn(coreAnalysisReport);
        when(coreClient.getCMSAnalysisReport(any())).thenReturn(cmsAnalysisReport);
        when(k8SServiceClient.getAnalysisReport(any())).thenReturn(pluginAnalysisReport);
    }

    public static void mockBundle(K8SServiceClient k8SServiceClient) {
        List<EntandoDeBundle> bundles = List.of(getTestBundle());
        when(k8SServiceClient.getBundlesInObservedNamespaces()).thenReturn(bundles);
        when(k8SServiceClient.getBundleWithName(any())).thenReturn(Optional.of(bundles.get(0)));
    }

    public static void mockPlugins(K8SServiceClient k8SServiceClient) {
        mockPlugins(k8SServiceClient, SUCCESSFUL);
    }

    public static void mockPlugins(K8SServiceClient k8sServiceClient, EntandoDeploymentPhase status) {
        EntandoCustomResourceStatus deploymentStatus = new EntandoCustomResourceStatus();
        deploymentStatus.updateDeploymentPhase(status, 1L);

        EntandoAppPluginLink plugin1 = new EntandoAppPluginLink(new ObjectMeta(),
                new EntandoAppPluginLinkSpec("", "", "", "entando-todomvcv1"),
                deploymentStatus);
        EntandoAppPluginLink plugin2 = new EntandoAppPluginLink(new ObjectMeta(),
                new EntandoAppPluginLinkSpec("", "", "", "entando-todomvcv2"),
                deploymentStatus);
        EntandoAppPluginLink plugin3 = new EntandoAppPluginLink(new ObjectMeta(),
                new EntandoAppPluginLinkSpec("", "", "", "custombasename"),
                deploymentStatus);

        when(k8sServiceClient.isPluginReadyToServeApp(any(), any())).thenReturn(true);
        when(k8sServiceClient.linkAppWithPlugin(any(), any(), any())).thenReturn(plugin1);
        when(k8sServiceClient.getLinkByName(any())).thenReturn(Optional.of(plugin1));
        when(k8sServiceClient.updatePlugin(any())).thenReturn(null);
        when(k8sServiceClient.getAppLinks(any())).thenReturn(List.of(plugin1, plugin2, plugin3));
    }

    @SneakyThrows
    public static byte[] readFromDEPackage(String bundleName) {
        try (final InputStream inputStream = TestInstallUtils.class.getClassLoader().getResourceAsStream(bundleName)) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Assertions.assertNotNull(inputStream);
                IOUtils.copy(inputStream, outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    public static void waitForInstallStatus(MockMvc mockMvc, JobStatus expected) {
        waitForInstallStatus(mockMvc, new JobStatus[]{expected});
    }

    public static void waitForInstallStatus(MockMvc mockMvc, JobStatus... expected) {
        waitForJobStatus(() -> getComponentLastJobStatusOfType(mockMvc, "todomvc", JobType.INSTALL.getStatuses()),
                expected);
    }

    public static void waitForUninstallStatus(MockMvc mockMvc, JobStatus expected) {
        waitForJobStatus(() -> getComponentLastJobStatusOfType(mockMvc, "todomvc", JobType.UNINSTALL.getStatuses()),
                expected);
    }

    public static void waitForJobStatus(Supplier<JobStatus> jobStatus, JobStatus... expected) {
        waitForJobStatus(jobStatus, Sets.newHashSet(expected));
    }

    public static void waitForJobStatus(Supplier<JobStatus> jobStatus, Set<JobStatus> expected) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> expected.contains(jobStatus.get()));
    }

    public static void waitForJobStatus(Supplier<JobStatus> jobStatus, JobStatus expected) {
        waitForJobStatus(jobStatus, new JobStatus[]{expected});
    }

    @SneakyThrows
    public static JobStatus getJobStatus(MockMvc mockMvc, String jobId) {
        MockHttpServletResponse response = mockMvc.perform(get("/jobs/" + jobId)
                .with(user("user")))
                .andReturn().getResponse();
        return JobStatus.valueOf(JsonPath.read(response.getContentAsString(), "$.payload.status"));
    }

    @SneakyThrows
    public static JobStatus getComponentLastJobStatusOfType(MockMvc mockMvc, String component,
            Set<JobStatus> possibleStatues) {
        List<String> allowedValues = possibleStatues.stream().map(JobStatus::name).collect(Collectors.toList());
        MockHttpServletResponse response = mockMvc.perform(get("/jobs"
                + "?sort=startedAt"
                + "&direction=DESC"
                + "&pageSize=1"
                + "&filters[0].attribute=status&filters[0].operator=eq&filters[0].allowedValues=" + String
                .join(",", allowedValues)
                + "&filters[1].attribute=componentId&filters[1].operator=eq&filters[1].value=" + component)
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(hasSize(1)))
                .andExpect(jsonPath("$.payload.[0].componentId").value(component))
                .andReturn().getResponse();
        return JobStatus.valueOf(JsonPath.read(response.getContentAsString(), "$.payload.[0].status"));
    }

    public static EntandoBundleJobEntity getJob(MockMvc mockMvc, String jobId) throws Exception {
        String responseContent = mockMvc.perform(get(JOBS_ENDPOINT + "/{id}", jobId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String status = JsonPath.read(responseContent, "$.payload.status");
        double progress = JsonPath.read(responseContent, "$.payload.progress");
        return EntandoBundleJobEntity.builder()
                .status(JobStatus.valueOf(status))
                .progress(progress)
                .build();
    }

    public static void verifyJobHasComponentAndStatus(MockMvc mockMvc, String jobId, JobStatus expectedStatus)
            throws Exception {
        mockMvc.perform(get(JOBS_ENDPOINT + "/{id}", jobId))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.componentId").value("todomvc"))
                .andExpect(jsonPath("payload.status").value(expectedStatus.toString()));
    }

    @SneakyThrows
    public static String simulateBundleDownloadError(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient,
            BundleDownloaderFactory factory) {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        factory.setDefaultSupplier(() -> null);
        mockBundle(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(500)));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        //        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_ERROR);
        return jobId;
    }

    @SneakyThrows
    public static String simulateSuccessfullyCompletedUninstall(MockMvc mockMvc, EntandoCoreClient coreClient) {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        setupComponentUsageToAllowUninstall(coreClient);
        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        //        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_COMPLETED);

        return jobId;
    }

    @SneakyThrows
    public static String simulateFailingInstall(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        mockFailingInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeSimulateFailingInstall(mockMvc, result);
    }

    @SneakyThrows
    public static String simulateFailingInstallWithInstallPlan(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        mockFailingInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(put(INSTALL_PLANS_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeSimulateFailingInstall(mockMvc, result);
    }


    private static void mockFailingInstall(EntandoCoreClient coreClient, K8SServiceClient k8sServiceClient,
            String bundleName) {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8sServiceClient);
        mockPlugins(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(bundleName))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        doThrow(new RestClientResponseException("error", 500, "Error", null, null, null))
                .when(coreClient).createPage(any(PageDescriptor.class));
    }

    private static String completeSimulateFailingInstall(MockMvc mockMvc, MvcResult result) throws Exception {

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ROLLBACK_ERROR,
                JobStatus.INSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    @SneakyThrows
    public static String simulateHugeAssetFailingInstall(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        mockHugeAssetFailingInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeHugeAssetFailingInstall(mockMvc, result);
    }

    @SneakyThrows
    public static String simulateHugeAssetFailingInstallWithInstallPlan(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {

        mockHugeAssetFailingInstall(coreClient, k8sServiceClient, bundleName);

        MvcResult result = mockMvc.perform(put(INSTALL_PLANS_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        return completeHugeAssetFailingInstall(mockMvc, result);
    }

    private static void mockHugeAssetFailingInstall(EntandoCoreClient coreClient, K8SServiceClient k8sServiceClient,
            String bundleName) {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8sServiceClient);
        mockPlugins(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(bundleName))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        doThrow(new RestClientResponseException("error", 413, "Error", null, null, null))
                .when(coreClient).createFile(any(FileDescriptor.class));
    }

    private static String completeHugeAssetFailingInstall(MockMvc mockMvc, MvcResult result) throws Exception {

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ROLLBACK_ERROR,
                JobStatus.INSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    @SneakyThrows
    public static String simulateFailingUninstall(MockMvc mockMvc, EntandoCoreClient coreClient) {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        setupComponentUsageToAllowUninstall(coreClient);
        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteFolder(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteContentModel(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteContentType(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteFragment(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteWidget(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deletePage(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deletePageModel(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient)
                .deleteLabel(any());

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

    }

    @SneakyThrows
    public static String simulateInProgressInstall(MockMvc mockMvc, EntandoCoreClient coreClient,
            K8SServiceClient k8sServiceClient, String bundleName) {
        Mockito.reset(coreClient);
        WireMock.reset();
        UniformDistribution delayDistribution = new UniformDistribution(200, 500);
        WireMock.setGlobalRandomDelay(delayDistribution);

        mockBundle(k8sServiceClient);
        mockPlugins(k8sServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(bundleName))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createPage(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createPageTemplate(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createWidget(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createFragment(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createContentType(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createContentTemplate(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createLabel(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createFolder(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).createFile(any());

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    @SneakyThrows
    public static String simulateInProgressUninstall(MockMvc mockMvc, EntandoCoreClient coreClient) {
        UniformDistribution delayDistribution = new UniformDistribution(200, 500);
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalRandomDelay(delayDistribution);

        setupComponentUsageToAllowUninstall(coreClient);
        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deletePage(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deletePageModel(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteWidget(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteFragment(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteContentType(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteContentModel(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteLabel(any());
        doSleep(Duration.ofMillis(delayDistribution.sampleMillis())).when(coreClient).deleteFolder(any());

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);
        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private static void setupComponentUsageToAllowUninstall(EntandoCoreClient coreClient) {
        when(coreClient.getGroupUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.GROUP));
        when(coreClient.getWidgetUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.WIDGET));
        when(coreClient.getPageUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE));
        when(coreClient.getContentModelUsage(anyString()))
                .thenReturn(new NoUsageComponent(ComponentType.CONTENT_TEMPLATE));
        when(coreClient.getPageModelUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE_TEMPLATE));
        when(coreClient.getFragmentUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.FRAGMENT));
        when(coreClient.getContentTypeUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.CONTENT_TYPE));
        when(coreClient.getCategoryUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.CATEGORY));
    }

    public static PagedMetadata<EntandoBundleJobEntity> getInstallJob(MockMvc mockMvc) throws Exception {
        return new ObjectMapper().readValue(mockMvc.perform(get(JOBS_ENDPOINT + "?component=todomvcV1&type=INSTALL"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<PagedMetadata<EntandoBundleJobEntity>>() {
                });
    }

    public static PagedMetadata<EntandoBundleJobEntity> getUninstallJob(MockMvc mockMvc) throws Exception {
        List<String> allowedValues = JobType.UNINSTALL.getStatuses().stream().map(JobStatus::name)
                .collect(Collectors.toList());
        return new ObjectMapper().readValue(mockMvc.perform(get("/jobs"
                        + "?sort=startedAt"
                        + "&direction=DESC"
                        + "&filters[0].attribute=status&filters[0].operator=eq&filters[0].allowedValues=" + String
                        .join(",", allowedValues)
                        + "&filters[1].attribute=componentId&filters[1].operator=eq&filters[1].value=todomvc"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<PagedMetadata<EntandoBundleJobEntity>>() {
                });
    }

    public static String requestProperty(final LoggedRequest request, final String property) {
        return JsonPath.read(new String(request.getBody()), property);
    }

    public static String requestCode(final LoggedRequest request) {
        return requestProperty(request, "code");
    }

    public static String requestPath(final LoggedRequest request) {
        return requestProperty(request, "path");
    }


}
