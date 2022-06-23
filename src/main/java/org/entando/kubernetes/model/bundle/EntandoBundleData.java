package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude
@Accessors(chain = true)
public class EntandoBundleData {

    private String id;
    private String bundleId;
    private String bundleName;
    private Set<String> componentTypes;
    private boolean installed;
    private String publicationUrl;

    public static EntandoBundleData fromEntity(EntandoBundleEntity entity) {
        return EntandoBundleData.builder()
                .id(entity.getId().toString())
                .bundleId(BundleUtilities.removeProtocolAndGetBundleId(entity.getRepoUrl()))
                .bundleName(entity.getBundleCode())
                .installed(entity.isInstalled())
                .componentTypes(entity.getType())
                .publicationUrl(entity.getRepoUrl())
                .build();
    }

}
