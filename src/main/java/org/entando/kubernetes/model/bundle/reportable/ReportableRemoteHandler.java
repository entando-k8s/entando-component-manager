package org.entando.kubernetes.model.bundle.reportable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * the remote handler type of the resource of which ask a report (entando-core, k8s-service, etc.).
 */
@Getter
@AllArgsConstructor
public enum ReportableRemoteHandler {

    ENTANDO_ENGINE,
    ENTANDO_CMS,
    ENTANDO_K8S_SERVICE;
}