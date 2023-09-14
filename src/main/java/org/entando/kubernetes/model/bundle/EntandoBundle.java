package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude
@Accessors(chain = true)
public class EntandoBundle {

    private String code;
    //private String organization;
    //private String ecrId; // hash(code + organization) = metadata.name
    private String title;
    private String description;
    private String repoUrl;
    private BundleType bundleType;
    private String thumbnail;
    private Set<String> componentTypes;
    private EntandoBundleJob installedJob;
    private EntandoBundleJob lastJob;
    private Boolean customInstallation;
    private EntandoBundleVersion latestVersion;
    private Map<String, String> annotations;

    @Default
    private List<EntandoBundleVersion> versions = new ArrayList<>();

    public boolean isInstalled() {
        return installedJob != null && installedJob.getStatus() == JobStatus.INSTALL_COMPLETED;
    }

    public Optional<EntandoBundleVersion> getLatestVersion() {
        return Optional.ofNullable(latestVersion);
    }

    public URL getRepoUrlAsURL() {
        if (ObjectUtils.isEmpty(repoUrl)) {
            return null;
        }

        try {
            return new URL(repoUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
