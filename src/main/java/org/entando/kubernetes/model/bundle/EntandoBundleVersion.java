package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude
public class EntandoBundleVersion {
    private String version;
    //private ZonedDateTime timestamp;

    public static EntandoBundleVersion fromEntity(EntandoDeBundleTag tag) {
        return EntandoBundleVersion.builder()
                .version(tag.getVersion())
                //.timestamp() TODO how to read from k8s custom model?
                .build();
    }
}
