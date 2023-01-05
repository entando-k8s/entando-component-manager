package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import io.micrometer.core.instrument.util.StringUtils;
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

    private String version;
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private Version semVersion;
    //private ZonedDateTime timestamp;

    public static EntandoBundleVersion fromEntity(EntandoDeBundleTag tag) {
        //.timestamp() TODO how to read from k8s custom model?
        try {
            return new EntandoBundleVersion().setVersion(tag.getVersion());
        } catch (IllegalArgumentException | ParseException ex) {
            log.info("Tag:'{}' skipped, value not compatible with semantic versioning or admitted values (e.g. v1.0.0)",
                    tag.getVersion());
            log.debug("error: ", ex);
            return null;
        }
    }

    public EntandoBundleVersion setVersion(String version) {
        this.version = version;
        this.semVersion = Version.valueOf(version.replaceAll("^v", ""));
        return this;
    }

    public boolean isSnapshot() {
        return StringUtils.isNotBlank(this.getSemVersion().getPreReleaseVersion());
    }
}
