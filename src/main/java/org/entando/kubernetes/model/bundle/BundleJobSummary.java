package org.entando.kubernetes.model.bundle;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BundleJobSummary {
    UUID id;
    JobStatus status;
    String version;

    public static BundleJobSummary fromEntity(EntandoBundleJob job) {
        if (job == null) {
            return new BundleJobSummary();
        }

        return BundleJobSummary.builder()
                .id(job.getId())
                .status(job.getStatus())
                .version(job.getComponentVersion())
                .build();
    }
}
