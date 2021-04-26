package org.entando.kubernetes.controller.digitalexchange.job.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.entando.kubernetes.assertionhelper.InstallPlanAssertionHelper;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
import org.entando.kubernetes.stubhelper.ReportableStubHelper;
import org.junit.jupiter.api.Test;

class InstallPlanTest {

    @Test
    void givenTwoInstallPlansTheyShouldMergeThemselves() {

        InstallPlan installPlanWithCats = InstallPlanStubHelper.stubInstallPlanWithCategories();
        InstallPlan installPlanWithFrags = InstallPlanStubHelper.stubInstallPlanWithFragments();

        InstallPlan mergedInstallPlan = installPlanWithCats.merge(installPlanWithFrags);

        InstallPlan expected = InstallPlanStubHelper.stubInstallPlanWithFragmentsAndCategories();
        InstallPlanAssertionHelper.assertOnInstallPlan(expected, mergedInstallPlan);

    }

    @Test
    void givenANullInstallPlanParamTheSameObjectShouldBeReturned() {

        InstallPlan installPlanWithCats = InstallPlanStubHelper.stubInstallPlanWithCategories();
        InstallPlan mergedInstallPlan = installPlanWithCats.merge(null);

        assertThat(mergedInstallPlan).isEqualTo(installPlanWithCats);
    }

    @Test
    void shouldNotBeCustomInstallionIfNoneOfTheComponentsHaveInstallActionEqualToSkip() {

        // empty InstallPlan
        InstallPlan emptyInstallPlan = new InstallPlan();
        assertThat(emptyInstallPlan.isCustomInstallation()).isFalse();

        // full install plan with only CREATE install actions
        InstallPlan notCustomInstallPlan = InstallPlanStubHelper.stubFullInstallPlanOnlyCreateAndOverrideInstallAction();
        assertThat(notCustomInstallPlan.isCustomInstallation()).isFalse();
    }

    @Test
    void shouldBeCustomInstallionIfAtLeastOneComponentHasInstallActionEqualToSkip() {

        InstallPlan lastCheckInstallPlan = InstallPlanStubHelper.stubFullInstallPlanOnlyCreateAndOverrideInstallAction();

        // should fail with skip
        lastCheckInstallPlan.getLanguages().get(ReportableStubHelper.LANG_CODE_1).setAction(InstallAction.SKIP);
        assertThat(lastCheckInstallPlan.isCustomInstallation()).isTrue();
    }
}
