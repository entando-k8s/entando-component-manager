package org.entando.kubernetes.controller.digitalexchange.job;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.NonNull;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
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
    private AuthorizationChecker authorizationChecker;

    private EntandoBundleOperationResourceController entandoBundleOperationResourceController;

    @BeforeEach
    public void setup() {
        entandoBundleOperationResourceController = new EntandoBundleOperationResourceController(
                kubernetesService, jobService, installService, uninstallService, installPlanValidator,
                authorizationChecker);
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
}
