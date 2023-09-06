package org.entando.kubernetes.model.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LastJobInstallStatus {

    private String status;

    public enum InstallationStatus {
        INSTALLING("installing"),
        UNINSTALLING("uninstalling");

        public final String label;

        private InstallationStatus(String label) {
            this.label = label;
        }
    }

}
