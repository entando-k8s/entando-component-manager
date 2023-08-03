package org.entando.kubernetes.controller.mockmvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.LocalDateTime;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.UninstallJobResult;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
class EntandoBundleOperationResourceControllerIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private EntandoBundleJobRepository jobRepository;
    private MockMvc mockMvc;
    private final String componentsUrl = "/components";
    private ObjectMapper mapper;

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
        jobRepository.deleteAll();
    }

    @Test
    void shouldReturnTheCurrentUninstallationStatusAndHttpStatus200IfUninstallationExists() throws Exception {
        // Given
        String componentId = "comp-id";
        EntandoBundleJobEntity entandoBundleJobEntity1 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED, componentId);
        EntandoBundleJobEntity entandoBundleJobEntity2 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.UNINSTALL_COMPLETED, componentId);
        jobRepository.save(entandoBundleJobEntity1);
        jobRepository.save(entandoBundleJobEntity2);
        // When the user sends the request
        // Then he gets the 200 http status and the uninstall job result
        ResultActions uninstallCompleted = mockMvc.perform(
                        get(componentsUrl + String.format("/%s/uninstall", componentId))
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.status", Matchers.equalTo("UNINSTALL_COMPLETED")));

    }

    @ParameterizedTest
    @ValueSource(strings = {"install", "installplans"})
    void shouldReturn404StatusIfInstallationIdDoesNotExists(String urlPath) throws Exception {
        // Given
        String componentId = "comp-id";
        // When the user sends the request
        // Then he gets the 400 http status and a proper message
        mockMvc.perform(get(componentsUrl + String.format("/%s/%s", componentId, urlPath))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(content().json(String.format("{\"message\":\"Job '%s' has not been found\"}", componentId)));
    }

    @Test
    void shouldReturn404StatusIfUninstallationIdDoesNotExists() throws Exception {
        // Given
        String componentId = "comp-id";
        EntandoBundleJobEntity entandoBundleJobEntity = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED, componentId);
        jobRepository.save(entandoBundleJobEntity);
        // When the user sends the request
        // Then he gets the 400 http status and a proper message
        mockMvc.perform(get(componentsUrl + String.format("/%s/uninstall", componentId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(content().json(String.format("{\"message\":\"Job '%s' has not been found\"}", componentId)));
    }

    @Test
    void shouldReturnOnlyDetailsRelevantToUninstallPlan() throws Exception {
        // Given
        String componentId = "comp-id";
        String installPlan = "install-plan";
        String uninstallErrors = "uninstall errors";
        String uninstallErrorMessage = "uninstall error message";
        String installErrorMessage = "install error message";
        int installErrorCode = 100;
        int uninstallErrorCode = 101;
        String userId = "user-id";
        double progress = 1.0;

        // simulate entity saved in DB
        EntandoBundleJobEntity entandoBundleJobEntity = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.UNINSTALL_ERROR, componentId);
        LocalDateTime now = LocalDateTime.now();
        entandoBundleJobEntity.setUserId(userId);
        entandoBundleJobEntity.setProgress(progress);
        entandoBundleJobEntity.setInstallPlan(installPlan);
        entandoBundleJobEntity.setInstallErrorCode(installErrorCode);
        entandoBundleJobEntity.setInstallErrorMessage(installErrorMessage);
        entandoBundleJobEntity.setUninstallErrors(uninstallErrors);
        entandoBundleJobEntity.setUninstallErrorMessage(uninstallErrorMessage);
        entandoBundleJobEntity.setUninstallErrorCode(uninstallErrorCode);
        jobRepository.save(entandoBundleJobEntity);

        // expected result payload
        UninstallJobResult expectedPayload = UninstallJobResult.builder()
                .id(entandoBundleJobEntity.getId())
                .componentId(componentId)
                .componentName(entandoBundleJobEntity.getComponentName())
                .componentVersion(entandoBundleJobEntity.getComponentVersion())
                .userId(userId)
                .progress(progress)
                .status(JobStatus.UNINSTALL_ERROR)
                .errorComponents(UninstallJobResult.deserialize(uninstallErrors))
                .uninstallErrorMessage(uninstallErrorMessage)
                .uninstallErrorCode(uninstallErrorCode)
                .build();

        // When the user sends the request
        // Then gets the 200 http status and the uninstall job result
        // only uninstall properties should be part of the response
        mockMvc.perform(
                        get(componentsUrl + String.format("/%s/uninstall", componentId))
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(new SimpleRestResponse<>(expectedPayload))));
    }

}
