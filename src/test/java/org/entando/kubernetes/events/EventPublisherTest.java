package org.entando.kubernetes.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Arrays;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
class EventPublisherTest {

    @Autowired
    private EntandoBundleJobRepository entandoBundleJobRepository;

    @BeforeEach
    public void setup() {
        EntandoBundleJobEntity completedOne = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED);
        EntandoBundleJobEntity error = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(JobStatus.INSTALL_ERROR);
        EntandoBundleJobEntity stuckInProgress = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_IN_PROGRESS);
        EntandoBundleJobEntity completedTwo = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED);

        entandoBundleJobRepository.saveAll(Arrays.asList(completedOne, error, stuckInProgress, completedTwo));
    }

    @AfterEach
    public void tearDown() {
        entandoBundleJobRepository.deleteAll();
    }

    @Test
    void shouldUnlockDatabaseBundleJobStatusRecordsOnStartup() {

        entandoBundleJobRepository.findAll()
                .forEach(job -> assertThat(job.getStatus()).isNotEqualTo(JobStatus.UNINSTALL_IN_PROGRESS));
    }
}
