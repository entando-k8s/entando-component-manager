package org.entando.kubernetes.controller.digitalexchange.job.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallRequest {

    @Default
    private final String version = "latest";

    @Default
    private final InstallAction conflictStrategy = InstallAction.CREATE;

    @Default
    private final InstallActionsByComponentType actions = new InstallActionsByComponentType();

    public enum InstallAction {
        CREATE,
        SKIP,
        OVERRIDE
    }
}
