package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Map.Entry;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class InstallPlanAssertionHelper {


    public static void assertOnInstallPlan(InstallPlan expected, InstallPlan actual) {

        assertThat(actual.getAssets()).containsOnly(toEntryArrayComponentInstallPlan(expected.getAssets()));
        assertThat(actual.getFragments()).containsOnly(toEntryArrayComponentInstallPlan(expected.getFragments()));
        assertThat(actual.getContents()).containsOnly(toEntryArrayComponentInstallPlan(expected.getContents()));
        assertThat(actual.getContentTemplates()).containsOnly(toEntryArrayComponentInstallPlan(expected.getContentTemplates()));
        assertThat(actual.getContentTypes()).containsOnly(toEntryArrayComponentInstallPlan(expected.getContentTypes()));
        assertThat(actual.getGroups()).containsOnly(toEntryArrayComponentInstallPlan(expected.getGroups()));
        assertThat(actual.getLabels()).containsOnly(toEntryArrayComponentInstallPlan(expected.getLabels()));
        assertThat(actual.getLanguages()).containsOnly(toEntryArrayComponentInstallPlan(expected.getLanguages()));
        assertThat(actual.getPages()).containsOnly(toEntryArrayComponentInstallPlan(expected.getPages()));
        assertThat(actual.getPageTemplates())
                .containsOnly(toEntryArrayComponentInstallPlan(expected.getPageTemplates()));
        assertThat(actual.getPlugins()).containsOnly(toEntryArrayComponentInstallPlan(expected.getPlugins()));
        assertThat(actual.getCategories()).containsOnly(toEntryArrayComponentInstallPlan(expected.getCategories()));
        assertThat(actual.getResources()).containsOnly(toEntryArrayComponentInstallPlan(expected.getResources()));
        assertThat(actual.getWidgets()).containsOnly(toEntryArrayComponentInstallPlan(expected.getWidgets()));
    }

    private static Entry[] toEntryArrayComponentInstallPlan(Map<String, ComponentInstallPlan> map) {
        return map.entrySet().toArray(Entry[]::new);
    }


    public static void assertOnComponentInstallPlan(ComponentInstallPlan componentInstallPlan, Status status,
            InstallAction installAction) {
        assertThat(componentInstallPlan.getStatus()).isEqualTo(status);
        assertThat(componentInstallPlan.getAction()).isEqualTo(installAction);
    }

    public static void assertOnNormalizedComponentInstallPlan(ComponentInstallPlan componentInstallPlan) {
        assertThat(componentInstallPlan.getStatus()).isEqualTo(Status.NEW);
        assertThat(componentInstallPlan.getAction()).isEqualTo(InstallAction.CREATE);
    }
}
