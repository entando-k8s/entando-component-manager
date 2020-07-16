package org.entando.kubernetes.model.bundle;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleJob;

@Data
@Builder
public class EntandoBundle {

    private String id;
    private String ecrId; // hash(code + organization) = metadata.name
    private String code;
    private String title;
    private String description;
    private String organization;
    private String thumbnail;
    private Set<String> componentTypes;
    private EntandoBundleJob installedJob;
    private EntandoBundleJob lastJob;
    private ZonedDateTime lastUpdate;
    private List<BundleVersion> versions;

    public boolean isInstalled() {
        return this.installedJob != null;
    }

    public Optional<BundleVersion> getLatestVersion() {
        return this.versions.stream().min(Comparator.comparing(BundleVersion::getVersion));
    }


}
