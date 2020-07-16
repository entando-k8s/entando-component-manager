package org.entando.kubernetes.controller.digitalexchange.component.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.entando.kubernetes.model.bundle.BundleJobSummary;
import org.entando.kubernetes.model.bundle.BundleVersion;

@Data
@Builder
public class EntandoBundleDTO {

    private String id;
    private String ecrId; // hash(code + organization) = metadata.name
    private String code;
    private String title;
    private String description;
    private String organization;
    private String thumbnail;
    private Set<String> componentTypes;
    private BundleJobSummary installedJob;
    private BundleJobSummary lastJob;
    private ZonedDateTime lastUpdate;
    private List<BundleVersion> versions;
}
