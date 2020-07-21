package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude
public class EntandoBundleVersion {
    private String version;
    //private ZonedDateTime timestamp;

    public static EntandoBundleVersion fromEntity(String version) {
        return EntandoBundleVersion.builder()
                .version(version)
                //.timestamp() TODO how to read from k8s custom model?
                .build();
    }
}
