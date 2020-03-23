package org.entando.kubernetes.controller;


import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
@Tag("component")
public class DigitalExchangeJobControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    private DigitalExchangeJobRepository jobRepository;

    @BeforeEach
    public void setup() {
        populateTestDatabase();
    }

    @AfterEach
    public void teardown() {
       jobRepository.deleteAll();
    }

    @Test
    public void shouldReturnAllJobsSortedByFinishTime() throws Exception {

        mvc.perform(get("/jobs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(hasSize(3)))
                .andExpect(jsonPath("$.payload[0].componentId").value("id1"))
                .andExpect(jsonPath("$.payload[0].status").value("UNINSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload[1].componentId").value("id2"))
                .andExpect(jsonPath("$.payload[1].status").value("INSTALL_COMPLETED"))
                .andExpect(jsonPath("$.payload[2].componentId").value("id1"))
                .andExpect(jsonPath("$.payload[2].status").value("INSTALL_COMPLETED"));
    }

    private void populateTestDatabase() {
        DigitalExchangeJob job1 = new DigitalExchangeJob();
        job1.setComponentId("id1");
        job1.setComponentName("my-bundle");
        job1.setDigitalExchange("local");
        job1.setProgress(1.0);
        job1.setComponentVersion("1.0.0");
        job1.setStartedAt(LocalDateTime.of(2020, Month.JANUARY, 10, 10, 30));
        job1.setFinishedAt(job1.getStartedAt().plusMinutes(1L));
        job1.setStatus(JobStatus.INSTALL_COMPLETED);

        jobRepository.save(job1);

        DigitalExchangeJob job1_uninstall = new DigitalExchangeJob();
        job1_uninstall.setComponentId("id1");
        job1_uninstall.setComponentName("my-bundle");
        job1_uninstall.setDigitalExchange("local");
        job1_uninstall.setComponentVersion("1.0.0");
        job1_uninstall.setProgress(1.0);
        job1_uninstall.setStartedAt(LocalDateTime.of(2020, Month.FEBRUARY, 2, 7, 23));
        job1_uninstall.setFinishedAt(job1_uninstall.getStartedAt().plusSeconds(30L));
        job1_uninstall.setStatus(JobStatus.UNINSTALL_COMPLETED);

        jobRepository.save(job1_uninstall);

        DigitalExchangeJob job2 = new DigitalExchangeJob();
        job2.setComponentId("id2");
        job2.setComponentName("my-other-bundle");
        job2.setDigitalExchange("external");
        job2.setComponentVersion("1.0.0");
        job2.setProgress(1.0);
        job2.setStartedAt(LocalDateTime.of(2020, Month.JANUARY, 14, 7, 23));
        job2.setFinishedAt(job2.getStartedAt().plusSeconds(30L));
        job2.setStatus(JobStatus.INSTALL_COMPLETED);

        jobRepository.save(job2);
    }


}
