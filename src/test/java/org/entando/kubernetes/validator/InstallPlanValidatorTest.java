package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.web.UnprocessableEntityException;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InstallPlanValidatorTest {

    private final InstallPlanValidator installPlanValidator = new InstallPlanValidator();

    @Test
    void shouldReturnTrueWithNullInstallPlan() {

        assertThat(installPlanValidator.validateInstallPlanOrThrow(null)).isTrue();
    }

    @Test
    void shouldThrowExceptionIfInstallPlanComponentHasEmptyKey() {

        InstallPlan installPlan = TestInstallUtils.mockInstallPlanWithActions();
        installPlan.setWidgets(new HashMap<>());

        // empty key
        installPlan.getWidgets().put("", InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW));
        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));

        // null key
        installPlan.getWidgets().remove("");
        installPlan.getWidgets().put(null, InstallPlanStubHelper.stubComponentInstallPlan(Status.NEW));
        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));
    }

    @Test
    void shouldThrowExceptionIfStatusNewAndInstallActionNOTCreate() {

        Map<String, ComponentInstallPlan> widgets = new HashMap<>();
        InstallPlan installPlan = InstallPlan.builder().widgets(widgets).build();

        // Status = NEW and InstallAction = OVERRIDE
        widgets.put("wid", ComponentInstallPlan.builder().status(Status.NEW).action(InstallAction.OVERRIDE).build());
        Assertions.assertThrows(UnprocessableEntityException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));
        widgets.remove("wid");

        // Status = NEW and InstallAction = SKIP
        widgets.put("wid", ComponentInstallPlan.builder().status(Status.NEW).action(InstallAction.SKIP).build());
        Assertions.assertThrows(UnprocessableEntityException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));
    }

    @Test
    void shouldThrowExceptionIfStatusNOTNewAndInstallActionCreate() {

        Map<String, ComponentInstallPlan> widgets = new HashMap<>();
        InstallPlan installPlan = InstallPlan.builder().widgets(widgets).build();

        // Status = DIFF and InstallAction = CREATE
        widgets.put("wid", ComponentInstallPlan.builder().status(Status.DIFF).action(InstallAction.CREATE).build());
        Assertions.assertThrows(UnprocessableEntityException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));
        widgets.remove("wid");

        // Status = NEW and InstallAction = SKIP
        widgets.put("wid", ComponentInstallPlan.builder().status(Status.EQUAL).action(InstallAction.CREATE).build());
        Assertions.assertThrows(UnprocessableEntityException.class,
                () -> installPlanValidator.validateInstallPlanOrThrow(installPlan));
    }
}
