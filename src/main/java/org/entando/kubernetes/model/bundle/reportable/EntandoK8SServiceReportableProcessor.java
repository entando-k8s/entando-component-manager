/**
 * this interface has to be implemented by those processors we want they create a report in order to manage
 * conflicts
 *
 * TARGETING ENTANDO K8S SERVICE
 */
package org.entando.kubernetes.model.bundle.reportable;

public interface EntandoK8SServiceReportableProcessor extends ReportableComponentProcessor {

    @Override
    default ReportableRemoteHandler getReportableRemoteHandler() {
        return ReportableRemoteHandler.ENTANDO_K8S_SERVICE;
    }
}