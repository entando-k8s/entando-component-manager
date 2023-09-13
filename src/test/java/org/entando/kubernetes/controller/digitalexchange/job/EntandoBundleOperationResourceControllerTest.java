package org.entando.kubernetes.controller.digitalexchange.job;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobNotFoundException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.UninstallJobResult;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.entando.kubernetes.stubhelper.EntandoBundleJobStubHelper;
import org.entando.kubernetes.validator.InstallPlanValidator;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundleOperationResourceControllerTest {

    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private EntandoBundleJobService jobService;
    @Mock
    private EntandoBundleInstallService installService;
    @Mock
    private EntandoBundleService bundleService;
    @Mock
    private EntandoBundleUninstallService uninstallService;
    @Mock
    private InstallPlanValidator installPlanValidator;
    @Mock
    private AuthorizationChecker authorizationChecker;

    private EntandoBundleOperationResourceController entandoBundleOperationResourceController;

    @BeforeEach
    public void setup() {
        entandoBundleOperationResourceController = new EntandoBundleOperationResourceController(
                kubernetesService, jobService, installService, uninstallService, installPlanValidator,
                authorizationChecker, bundleService);
    }

    @Test
    void shouldApplyValidationBeforeAnythingDuringTheInstallWithInstallPlan() {

        when(installPlanValidator.validateInstallPlanOrThrow(any())).thenThrow(EntandoComponentManagerException.class);

        try {
            entandoBundleOperationResourceController.installWithInstallPlan("jwt", "bundleId",
                    new InstallWithPlansRequest());
        } catch (EntandoComponentManagerException e) {
            e.printStackTrace();
        }

        verify(installPlanValidator, times(1)).validateInstallPlanOrThrow(any());
        verify(kubernetesService, times(0)).fetchBundleByName(anyString());
        verify(jobService, times(0)).findCompletedOrConflictingInstallJob(any());
    }

    @Test
    void shouldReturnErrorDuringTheInstallWithInstallReqCreateOnInstalledBundle() {
        final String bundleId = "bundleId";
        EntandoBundle bundle = EntandoBundle.builder()
                .installedJob(EntandoBundleJob.builder().status(JobStatus.INSTALL_COMPLETED).build()).code(bundleId)
                .build();
        when(bundleService.getInstalledBundle(bundleId)).thenReturn(Optional.of(bundle));
        final InstallRequest installReqCreate = InstallRequest.builder().conflictStrategy(InstallAction.CREATE).build();

        Assert.assertThrows(JobConflictException.class,
                () -> entandoBundleOperationResourceController.install("jwt", bundleId, installReqCreate));

        final InstallRequest installReqNull = InstallRequest.builder().conflictStrategy(null).build();
        Assert.assertThrows(JobConflictException.class,
                () -> entandoBundleOperationResourceController.install("jwt", bundleId, installReqNull));

    }

    @Test
    void shouldThrowJobNotFoundExceptionWith404StatusIfUninstallationIdDoesNotExistsOnGet() {
        // Given
        final String componentId = "componentId";
        when(jobService.getJobs(componentId)).thenReturn(List.of());
        // Then
        JobNotFoundException jobNotFoundException = Assertions.assertThrows(JobNotFoundException.class,
                () -> entandoBundleOperationResourceController.getLastUninstallJob(componentId));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, jobNotFoundException.getStatus());
    }

    @Test
    void shouldReturnTheCurrentUninstallationStatusOnGetIfUninstallationExistsAndItWasTheLastOperationDone() {
        // Given
        final String componentId = "comp-id";
        EntandoBundleJobEntity jobEntity1 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED, componentId);
        EntandoBundleJobEntity jobEntity2 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.UNINSTALL_COMPLETED);
        // here the order of job entities is important because the "jobService.getJobs" method stubbed in the real code
        // retrieves all the related job entities ordered by start date descendant. So the last one matters
        when(jobService.getJobs(componentId)).thenReturn(List.of(jobEntity2, jobEntity1));
        // When
        SimpleRestResponse<UninstallJobResult> lastUninstallJob = entandoBundleOperationResourceController.getLastUninstallJob(
                componentId);
        // Then
        Assertions.assertEquals(JobStatus.UNINSTALL_COMPLETED, lastUninstallJob.getPayload().getStatus());
    }

    @Test
    void shouldThrowJobNotFoundExceptionOnGetIfUninstallationExistsButItWasNotTheLastOperationDone() {
        // Given
        final String componentId = "comp-id";
        EntandoBundleJobEntity jobEntity1 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED, componentId);
        EntandoBundleJobEntity jobEntity2 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.UNINSTALL_COMPLETED);
        // here the order of job entities is important because the "jobService.getJobs" method stubbed in the real code
        // retrieves all the related job entities ordered by start date descendant. So the last one matters
        when(jobService.getJobs(componentId)).thenReturn(List.of(jobEntity1, jobEntity2));
        // When/Then
        Assertions.assertThrows(JobNotFoundException.class,
                () -> entandoBundleOperationResourceController.getLastUninstallJob(
                        componentId));
    }

    @Test
    void shouldThrowJobNotFoundExceptionOnGetIfUninstallationDoNotExists() {
        // Given
        final String componentId = "comp-id";
        EntandoBundleJobEntity jobEntity1 = EntandoBundleJobStubHelper.stubEntandoBundleJobEntity(
                JobStatus.INSTALL_COMPLETED, componentId);
        when(jobService.getJobs(componentId)).thenReturn(List.of(jobEntity1));
        // When/Then
        Assertions.assertThrows(JobNotFoundException.class,
                () -> entandoBundleOperationResourceController.getLastUninstallJob(
                        componentId));
    }
}
