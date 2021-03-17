package org.entando.kubernetes.controller.digitalexchange.job.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Map;
import org.entando.kubernetes.assertionhelper.InstallPlanAssertionHelper;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
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
}
