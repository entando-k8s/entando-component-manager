package org.entando.kubernetes.controller.digitalexchange.job.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallRequest {

    @Default
    private final String version = BundleUtilities.LATEST_VERSION;

    @Default
    private final InstallAction conflictStrategy = InstallAction.CREATE;

}
