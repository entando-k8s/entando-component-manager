package org.entando.kubernetes.model.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class EntandoBundleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void testGetLatestVersionWithLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);

        assertEquals("v0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithoutLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove the starting v from versions
        entandoBundle.getVersions()
                .forEach(this::removeLeadingV);

        assertEquals("0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionMixingLeadingVWithFinalElementWithV() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove some starting v from versions
        this.removeLeadingV(entandoBundle.getVersions().get(4));
        this.removeLeadingV(entandoBundle.getVersions().get(6));

        assertEquals("v0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionMixingLeadingVWithFinalElementWithoutV() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        // remove some starting v from versions
        this.removeLeadingV(entandoBundle.getVersions().get(4));
        this.removeLeadingV(entandoBundle.getVersions().get(12));

        assertEquals("0.0.13", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlpha() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("0.0.14-alpha"));

        assertEquals("0.0.14-alpha", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlphaAndLeadingV() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-alpha"));

        assertEquals("v0.0.14-alpha", entandoBundle.getLatestVersion().get().getVersion());
    }

    @Test
    void testGetLatestVersionWithAlphaAndStable() throws IOException {

        EntandoBundle entandoBundle = objectMapper.readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"), EntandoBundle.class);
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14-alpha"));
        entandoBundle.getVersions().add(new EntandoBundleVersion().setVersion("v0.0.14"));

        assertEquals("v0.0.14", entandoBundle.getLatestVersion().get().getVersion());
    }


    /**
     *
     * @param entandoBundleVersion
     */
    private void removeLeadingV(EntandoBundleVersion entandoBundleVersion) {
        entandoBundleVersion.setVersion(entandoBundleVersion.getVersion().replaceAll("^v", ""));
    }

}
