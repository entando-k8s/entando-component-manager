package org.entando.kubernetes.controller.mockmvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.TestTenantConfiguration;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class,
                TestTenantConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class EntandoBundleJobControllerTest {

    MockMvc mvc;
    Map<UUID, EntandoBundleJobEntity> jobs;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private EntandoBundleJobRepository jobRepository;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        populateTestDatabase();
    }

    @AfterEach
    public void teardown() {
        jobRepository.deleteAll();
    }

    @Test
    public void shouldReturnAllJobsSortedByStartTime() throws Exception {

        mvc.perform(get("/jobs?sort=startedAt&direction=DESC").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(hasSize(4)))
                .andExpect(jsonPath("$.payload[0].componentId").value("id1"))
                .andExpect(jsonPath("$.payload[0].status").value("INSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload[0].startedAt").value("2020-03-05T14:30:00"))
                .andExpect(jsonPath("$.payload[1].componentId").value("id1"))
                .andExpect(jsonPath("$.payload[1].status").value("UNINSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload[1].startedAt").value("2020-02-02T07:23:00"))
                .andExpect(jsonPath("$.payload[2].componentId").value("id2"))
                .andExpect(jsonPath("$.payload[2].status").value("INSTALL_IN_PROGRESS"))
                .andExpect(jsonPath("$.payload[2].startedAt").value("2020-01-14T07:23:00"))
                .andExpect(jsonPath("$.payload[3].componentId").value("id1"))
                .andExpect(jsonPath("$.payload[3].status").value("INSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload[3].startedAt").value("2020-01-10T10:30:00"));
    }

    @Test
    public void shouldReturnJobById() throws Exception {

        EntandoBundleJobEntity job = jobs.entrySet().stream().findFirst().map(Entry::getValue).get();

        mvc.perform(get("/jobs/{id}", job.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.componentId").value(job.getComponentId()))
                .andExpect(jsonPath("$.payload.status").value(job.getStatus().toString()));
    }

    @Test
    public void shouldReturnNotFoundWithNonExistentId() throws Exception {

        UUID jobId = UUID.randomUUID();

        mvc.perform(get("/jobs/{id}", jobId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnLastJobOfType() throws Exception {

        String componentId = "id1";

        mvc.perform(
                get("/jobs"
                        + "?filters[0].attribute=componentId&filters[0].value=id1&filters[0].operator=eq"
                        + "&filters[1].attribute=status&filters[1].operator=eq&filters[1].allowedValues=INSTALL_COMPLETED,"
                        + "INSTALL_IN_PROGRESS"
                        + "&pageSize=1&sort=startedAt&direction=DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.[0].componentId").value(componentId))
                .andExpect(jsonPath("$.payload.[0].status").value(JobStatus.INSTALL_COMPLETED.toString()))
                .andExpect(jsonPath("$.payload.[0].finishedAt").value("2020-03-05T14:32:00"));
    }

    @Test
    public void shouldReturnLastJobWithStatus() throws Exception {

        String componentId = "id1";

        mvc.perform(get("/jobs"
                + "?filters[0].attribute=componentId&filters[0].value=id1&filters[0].operator=eq"
                + "&filters[1].attribute=status&filters[1].operator=eq&filters[1].value=UNINSTALL_COMPLETED"
                + "&pageSize=1&sort=startedAt&direction=DESC")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.[0].componentId").value(componentId))
                .andExpect(jsonPath("$.payload.[0].status").value("UNINSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload.[0].startedAt").value("2020-02-02T07:23:00"))
                .andExpect(jsonPath("$.payload.[0].finishedAt").value("2020-02-02T07:23:30"));
    }

    private void populateTestDatabase() {
        jobs = new HashMap<>();

        EntandoBundleJobEntity job1 = new EntandoBundleJobEntity();
        job1.setComponentId("id1");
        job1.setComponentName("my-bundle");
        job1.setProgress(1.0);
        job1.setComponentVersion("1.0.0");
        job1.setStartedAt(LocalDateTime.of(2020, Month.JANUARY, 10, 10, 30));
        job1.setFinishedAt(job1.getStartedAt().plusMinutes(1L));
        job1.setStatus(JobStatus.INSTALL_COMPLETED);

        EntandoBundleJobEntity savedJob = jobRepository.save(job1);
        jobs.put(savedJob.getId(), savedJob);

        EntandoBundleJobEntity job2 = new EntandoBundleJobEntity();
        job2.setComponentId("id2");
        job2.setComponentName("my-other-bundle");
        job2.setComponentVersion("1.0.0");
        job2.setProgress(0.5);
        job2.setStartedAt(LocalDateTime.of(2020, Month.JANUARY, 14, 7, 23));
        job2.setFinishedAt(null);
        job2.setStatus(JobStatus.INSTALL_IN_PROGRESS);

        EntandoBundleJobEntity savedJob2 = jobRepository.save(job2);
        jobs.put(savedJob2.getId(), savedJob2);

        EntandoBundleJobEntity job1Uninstall = new EntandoBundleJobEntity();
        job1Uninstall.setComponentId("id1");
        job1Uninstall.setComponentName("my-bundle");
        job1Uninstall.setComponentVersion("1.0.0");
        job1Uninstall.setProgress(1.0);
        job1Uninstall.setStartedAt(LocalDateTime.of(2020, Month.FEBRUARY, 2, 7, 23));
        job1Uninstall.setFinishedAt(job1Uninstall.getStartedAt().plusSeconds(30L));
        job1Uninstall.setStatus(JobStatus.UNINSTALL_COMPLETED);

        EntandoBundleJobEntity savedJobUninstall = jobRepository.save(job1Uninstall);
        jobs.put(savedJobUninstall.getId(), savedJobUninstall);

        EntandoBundleJobEntity job1Reinstall = new EntandoBundleJobEntity();
        job1Reinstall.setComponentId("id1");
        job1Reinstall.setComponentName("my-bundle");
        job1Reinstall.setProgress(1.0);
        job1Reinstall.setComponentVersion("1.0.0");
        job1Reinstall.setStartedAt(LocalDateTime.of(2020, Month.MARCH, 5, 14, 30));
        job1Reinstall.setFinishedAt(job1Reinstall.getStartedAt().plusMinutes(2L));
        job1Reinstall.setStatus(JobStatus.INSTALL_COMPLETED);

        EntandoBundleJobEntity savedJob1Reinstall = jobRepository.save(job1Reinstall);
        jobs.put(savedJob1Reinstall.getId(), savedJob1Reinstall);

    }

}
