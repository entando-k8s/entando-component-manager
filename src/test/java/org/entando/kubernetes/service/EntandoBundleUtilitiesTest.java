package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;

import org.entando.kubernetes.model.bundle.EntandoComponentBundleVersion;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntandoBundleUtilitiesTest {

    @Test
    public void shouldReturnVersionDirectly() {
        EntandoComponentBundleVersion version = BundleUtilities.getBundleVersionOrFail(getTestBundle(), "0.0.1");
        assertThat(version.getVersion()).isEqualTo("0.0.1");
    }

    @Test
    public void shouldThrowAnErrorAsVersionIsNotDefined() {
        Exception ex = Assertions.assertThrows(RuntimeException.class, () -> {
            BundleUtilities.getBundleVersionOrFail(getTestBundle(), "first");
        });

        assertThat(ex.getMessage()).isEqualTo("Invalid version 'first' for bundle 'my-bundle'");
    }

    @Test
    public void shouldReturnLatestVersion() {
        EntandoComponentBundleVersion version = BundleUtilities.getBundleVersionOrFail(getTestBundle(), "latest");
        assertThat(version.getVersion()).isEqualTo("0.0.1");
    }

    @Test
    public void shouldAcceptSemVersionsWithStartingWithV() {
        assertThat(BundleUtilities.isSemanticVersion("v0.0.1")).isTrue();
    }

    @Test
    public void shouldCheckIsLatestVersion() {
        assertThat(BundleUtilities.isLatestVersion(null)).isTrue();
        assertThat(BundleUtilities.isLatestVersion("latest")).isTrue();
        assertThat(BundleUtilities.isLatestVersion("LATEST")).isFalse();
    }

}
