package org.entando.kubernetes.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.entando.kubernetes.utils.TenantSecurityKeycloakMockServerJunitExt;
import org.entando.kubernetes.utils.TenantTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("in-process")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ExtendWith({TenantContextJunitExt.class, TenantSecurityKeycloakMockServerJunitExt.class})
class EventPublisherTest {

    @Autowired
    private EntandoBundleJobRepository entandoBundleJobRepository;

    private List<EntandoBundleJobEntity> savedEntities;

    @BeforeEach
    public void setup() {
        EntandoBundleJobEntity completedOne = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED);
        EntandoBundleJobEntity error = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(JobStatus.INSTALL_ERROR);
        EntandoBundleJobEntity stuckInProgress = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_IN_PROGRESS);
        EntandoBundleJobEntity completedTwo = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED);

        savedEntities = entandoBundleJobRepository.saveAll(Arrays.asList(completedOne, error, stuckInProgress, completedTwo));

    }

    @AfterEach
    public void tearDown() {
        TenantTestUtils.executeInPrimaryTenantContext(() -> {
            savedEntities.stream().forEach(s -> {
                        try {
                            entandoBundleJobRepository.delete(s);
                        } catch (Exception ex) {
                            log.error("unable to delete entity:'{}'", s, ex);
                        }
                    }
            );
        });
    }

    @Test
    void shouldUnlockDatabaseBundleJobStatusRecordsOnStartup() {

        entandoBundleJobRepository.findAll()
                .forEach(job -> assertThat(job.getStatus()).isNotEqualTo(JobStatus.UNINSTALL_IN_PROGRESS));
    }
}
