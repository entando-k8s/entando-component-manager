package org.entando.kubernetes.controller.digitalexchange.job;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.validator.BundleRepositoryUrlValidator;
import org.entando.kubernetes.validator.InstallPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private EntandoBundleUninstallService uninstallService;
    @Mock
    private InstallPlanValidator installPlanValidator;
    @Mock
    private BundleRepositoryUrlValidator bundleRepositoryUrlValidator;

    private EntandoBundleOperationResourceController entandoBundleOperationResourceController;

    @BeforeEach
    public void setup() {
        entandoBundleOperationResourceController = new EntandoBundleOperationResourceController(
                kubernetesService, jobService, installService, uninstallService, installPlanValidator,
                bundleRepositoryUrlValidator);
    }

    @Test
    void shouldApplyValidationBeforeAnythingDuringTheInstallWithInstallPlan() {

        when(installPlanValidator.validateInstallPlanOrThrow(any())).thenThrow(EntandoComponentManagerException.class);

        try {
            entandoBundleOperationResourceController.installWithInstallPlan("bundleId", new InstallWithPlansRequest());
        } catch (EntandoComponentManagerException e) {
            e.printStackTrace();
        }

        verify(installPlanValidator, times(1)).validateInstallPlanOrThrow(any());
        verify(kubernetesService, times(0)).fetchBundleByName(anyString());
        verify(jobService, times(0)).findCompletedOrConflictingInstallJob(any());
    }

    @Test
    void shouldThrowExceptionWhileReceivingATagWithAnInvalidRepoUrlToInstall() {
        when(bundleRepositoryUrlValidator.validateOrThrow(anyString())).thenThrow(EntandoValidationException.class);

        EntandoDeBundle entandoDeBundle = TestEntitiesGenerator.getTestBundle();
        when(kubernetesService.fetchBundleByName(anyString())).thenReturn(Optional.of(entandoDeBundle));

        final InstallRequest installRequest = InstallRequest.builder().version("0.0.1").build();

        assertThrows(EntandoValidationException.class,
                () -> entandoBundleOperationResourceController.install(BundleStubHelper.BUNDLE_NAME, installRequest));
    }
}
