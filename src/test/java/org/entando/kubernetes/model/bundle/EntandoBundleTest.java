package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EntandoBundleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetLatestVersionWithLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);

        assertEquals("v0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithoutLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove the starting v from versions
        entandoBundle.getVersions()
                .forEach(this::removeLeadingV);

        assertEquals("0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionMixingLeadingVWithFinalElementWithV() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove some starting v from versions
        this.removeLeadingV(entandoBundle.getVersions().get(4));
        this.removeLeadingV(entandoBundle.getVersions().get(6));

        assertEquals("v0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionMixingLeadingVWithFinalElementWithoutV() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove some starting v from versions
        this.removeLeadingV(entandoBundle.getVersions().get(4));
        this.removeLeadingV(entandoBundle.getVersions().get(12));

        assertEquals("0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlpha() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("0.0.14-alpha"));

        assertEquals("0.0.14-alpha", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlphaAndLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-alpha"));

        assertEquals("v0.0.14-alpha", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlphaAndStable() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-alpha"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14"));

        assertEquals("v0.0.14", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testExpectedVersionSort() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-beta"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-alpha"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-rc.1"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-rc.2"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-snapshot"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14"));

        List<String> sortedVersions = entandoBundle.getVersions().stream()
                .sorted(Comparator.comparing(EntandoBundleVersion::getSemVersion, Version::compareTo).reversed())
                .map(EntandoBundleVersion::getVersion)
                .collect(Collectors.toList());
        assertThat(sortedVersions)
                .startsWith("v0.0.14", "v0.0.14-snapshot", "v0.0.14-rc.2", "v0.0.14-rc.1", "v0.0.14-beta", "v0.0.14-alpha");
    }

    private void removeLeadingV(EntandoBundleVersion entandoBundleVersion) {
        entandoBundleVersion.setVersion(entandoBundleVersion.getVersion().replaceAll("^v", ""));
    }

}
