package org.entando.kubernetes.controller.digitalexchange.component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJob;

@Data
@Builder
public class EntandoBundle {
    private String id;
    private String name;
    private Set<String> type;
    private EntandoBundleJob job; //TODO remove or convert to DTO
    private Date lastUpdate;
    private String version;
    private String description;
    private String image;
    private boolean installed;
    private String signature;
    private Map<String, String> metadata;

    public static EntandoBundle fromEntity(EntandoBundleEntity entity) {
        return EntandoBundle.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .job(entity.getJob())
                .lastUpdate(entity.getLastUpdate())
                .version(entity.getVersion())
                .description(entity.getDescription())
                .image(entity.getImage())
                .installed(entity.isInstalled())
                .signature(entity.getSignature())
                .metadata(entity.getMetadata())
                .build();
    }

}
