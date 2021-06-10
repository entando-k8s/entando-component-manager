package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

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
    void testExpectedVersionSort() throws IOException {

        EntandoBundle entandoBundle = objectMapper
                .readValue(new File("src/test/resources/payloads/k8s-svc/bundles/bundle_with_versions.json"),
                        EntandoBundle.class);
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
                .startsWith("v0.0.14", "v0.0.14-snapshot", "v0.0.14-rc.2", "v0.0.14-rc.1", "v0.0.14-beta",
                        "v0.0.14-alpha");
    }
}
