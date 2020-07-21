package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude
/*
   TODO: Commented out code will be addressed in future sprints (f.leandro, l.cherubin)
 */
public class EntandoBundle {
    //private String ecrId; // hash(code + organization) = metadata.name
    private String code;
    private String title;
    private String description;
    //private String organization;
    private String thumbnail;
    private Set<String> componentTypes;
    private EntandoBundleJob installedJob;
    private EntandoBundleJob lastJob;

    @Default
    private List<EntandoBundleVersion> versions = new ArrayList<>();

    public boolean isInstalled() {
        return installedJob != null && installedJob.getStatus() == JobStatus.INSTALL_COMPLETED;
    }

    public Optional<EntandoBundleVersion> getLatestVersion() {
        return this.versions.stream().max(Comparator.comparing(EntandoBundleVersion::getVersion));
    }

}
