package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
@Accessors(chain = true)
public class EntandoBundleVersion {

    public static final String INVALID_VERSION_ERROR = "Tag:'{}' skipped, value not compatible with semantic versioning or admitted values (e.g. v1.0.0)";

    private String version;
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private Version semVersion;

    public static EntandoBundleVersion fromEntity(EntandoDeBundleTag tag) {
        return fromString(tag.getVersion())
                .map(v -> new EntandoBundleVersion(tag.getVersion(), v))
                .orElse(null);
    }

    public EntandoBundleVersion setVersion(String version) {
        return fromString(version)
                .map(v -> {
                    this.semVersion = v;
                    this.version = version;
                    return this;
                })
                .orElse(null);
    }

    public static Optional<Version> fromString(String version) {
        try {
            return Optional.of(Version.valueOf(version.replaceAll("^v", "")));
        } catch (IllegalArgumentException | ParseException ex) {
            log.info(INVALID_VERSION_ERROR, version);
            log.debug("error: ", ex);
            return Optional.empty();
        }
    }

    public boolean isSnapshot() {
        return StringUtils.isNotBlank(this.getSemVersion().getPreReleaseVersion());
    }

    public static boolean isSemanticVersion(String version) {
        try {
            Version.valueOf(version);
            return true;
        } catch (IllegalArgumentException | ParseException ex) {
            return false;
        }
    }
}
