package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.util.Set;
import org.apache.commons.compress.utils.Sets;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.hamcrest.core.IsNull;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.CollectionUtils;

public class BundleAssertionHelper {


    public static void assertOnBundleAndDeBundle(EntandoBundle bundle, EntandoDeBundle deBundle, BundleType bundleType,
            EntandoBundleJob installedJob, EntandoBundleJob lastJob, Boolean customInstallation,
            EntandoBundleVersion latestVersion) {

        final ObjectMeta metadata = deBundle.getMetadata();
        final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();

        assertThat(bundle.getCode()).isEqualTo(metadata.getName());
        assertThat(bundle.getTitle()).isEqualTo(details.getName());
        assertThat(bundle.getDescription()).isEqualTo(details.getDescription());
        assertThat(bundle.getBundleType()).isEqualTo(bundleType);
        assertThat(bundle.getThumbnail()).isEqualTo(details.getThumbnail());
        assertOnComponentTypes(bundle, deBundle);
        if (installedJob != null) {
            assertThat(bundle.getInstalledJob()).isEqualTo(installedJob);
        }
        if (lastJob != null) {
            assertThat(bundle.getLastJob()).isEqualTo(lastJob);
        }
        assertThat(bundle.getCustomInstallation()).isEqualTo(customInstallation);
        assertThat(bundle.getLatestVersion().get().getVersion()).isEqualTo(latestVersion.getVersion());
    }

    public static void assertOnComponentTypes(EntandoBundle bundle, EntandoDeBundle deBundle) {

        if (CollectionUtils.isEmpty(deBundle.getMetadata().getLabels())
                && bundle.getComponentTypes().size() == 1) {    // 1 is valid because the bundle item is always present
            return;
        }

        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        deBundle.getMetadata().getLabels()
                .keySet().stream()
                .filter(ComponentType::isValidType)
                .forEach(bundleComponentTypes::add);

        assertThat(bundle.getComponentTypes()).containsExactlyInAnyOrder(
                bundleComponentTypes.toArray(new String[bundleComponentTypes.size()]));
    }


    public static void assertOnEntandoBundle(ResultActions result, EntandoBundle bundle) throws Exception {

        result.andExpect(jsonPath("$.payload.code", is(bundle.getCode())))
                .andExpect(jsonPath("$.payload.title", is(bundle.getTitle())))
                .andExpect(jsonPath("$.payload.description", is(bundle.getDescription())))
                .andExpect(jsonPath("$.payload.repoUrl", is(bundle.getRepoUrl())))
                .andExpect(jsonPath("$.payload.bundleType", is(bundle.getBundleType().getType())))
                .andExpect(jsonPath("$.payload.thumbnail", is(bundle.getThumbnail())))
                .andExpect(jsonPath("$.payload.componentTypes", hasSize(bundle.getComponentTypes().size())))
                .andExpect(jsonPath("$.payload.componentTypes", hasItems(bundle.getComponentTypes().toArray(
                        new String[0]))))
                .andExpect(jsonPath("$.payload.installedJob", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.lastJob", IsNull.nullValue()))
                .andExpect(
                        jsonPath("$.payload.latestVersion.version", is(bundle.getLatestVersion().get().getVersion())));

        if (bundle.getCustomInstallation() == null) {
            result.andExpect(jsonPath("$.payload.customInstallation", IsNull.nullValue()));
        } else {
            result.andExpect(jsonPath("$.payload.customInstallation", is(bundle.getCustomInstallation())));
        }
    }
}
