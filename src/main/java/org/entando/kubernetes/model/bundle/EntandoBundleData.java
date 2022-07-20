package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude
@Accessors(chain = true)
public class EntandoBundleData implements Labeled {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private String id;
    private String bundleId;
    private String bundleCode;
    private Set<String> componentTypes;
    private boolean installed;
    private String publicationUrl;
    @JsonRawValue
    private String descriptorExt;
    private Labels labels;

    public void setLabels(Labels labels) {
        this.labels = labels;
    }

    public static EntandoBundleData fromEntity(EntandoBundleEntity entity) {

        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(entity.getRepoUrl());

        return EntandoBundleData.builder()
                .id(entity.getId().toString())
                .bundleId(bundleId)
                .bundleCode(entity.getBundleCode())
                .installed(entity.isInstalled())
                .componentTypes(entity.getType())
                .publicationUrl(entity.getRepoUrl())
                .labels(new Labels(Labeled.getPbcLabelsFrom(entity)))
                .descriptorExt(entity.getExt())
                .build();
    }
}
