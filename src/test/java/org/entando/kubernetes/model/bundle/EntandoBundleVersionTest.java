package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoBundleVersionTest {

    @Test
    void shouldCreateTheExpectedValue() {
        final String dockerImage = "docker://docker.io/entando/testimage";
        final String dockerImageDigest = "sha256:420c584cb6a6a64cea490a921873d7ca36f88692da37569708de0e1e48cd10fc";
        final String keyInput = "input";
        final String keyExpected = "expected";
        List.of(Map.of(keyInput, "v1.0.0", keyExpected, "v1.0.0"), Map.of("input", "1.0.0", "expected", "1.0.0"))
                .forEach(m -> {
                    EntandoDeBundleTag tagDockerV = new EntandoDeBundleTag(m.get(keyInput), dockerImageDigest,
                                    dockerImageDigest, dockerImage);

                            EntandoBundleVersion ver1 = EntandoBundleVersion.fromEntity(tagDockerV);
                            assertThat(ver1.getVersion()).isEqualTo(m.get(keyExpected));
                        }

                );
    }

    @Test
    void shouldReturnError() {
        final String dockerImage = "docker://docker.io/entando/testimage";
        final String dockerImageDigest = "sha256:420c584cb6a6a64cea490a921873d7ca36f88692da37569708de0e1e48cd10fc";
        final String keyInput = "input";
        List.of(Map.of("input", "latest"), Map.of("input", "main")).forEach(m -> {
                    EntandoDeBundleTag tagDockerV = new EntandoDeBundleTag(m.get(keyInput), dockerImageDigest,
                            dockerImageDigest, dockerImage);

                    assertThat(EntandoBundleVersion.fromEntity(tagDockerV)).isNull();
                }

        );
    }

    @Test
    void shouldHandleCorrectlySnapshotVersion() {
        final String dockerImage = "docker://docker.io/entando/testimage";
        final String dockerImageDigest = "sha256:420c584cb6a6a64cea490a921873d7ca36f88692da37569708de0e1e48cd10fc";
        List.of("v1.1.0+12", "v1.1.0", "1.1.0").forEach(version -> {
            EntandoDeBundleTag tagDockerV = new EntandoDeBundleTag(version, dockerImageDigest, dockerImageDigest,
                    dockerImage);
            assertThat(EntandoBundleVersion.fromEntity(tagDockerV).isSnapshot()).isFalse();
        });

        List.of("v1.1.0-PRE-RELEASE", "v1.1.0-SNAPSHOT", "1.1.0-SNAPSHOT+KB12-GG", "1.1.0-ENG-3502-PR-11")
                .forEach(version -> {
                    EntandoDeBundleTag tagDockerV = new EntandoDeBundleTag(version, dockerImageDigest,
                            dockerImageDigest, dockerImage);
                    assertThat(EntandoBundleVersion.fromEntity(tagDockerV).isSnapshot()).isTrue();
                });

    }
}
