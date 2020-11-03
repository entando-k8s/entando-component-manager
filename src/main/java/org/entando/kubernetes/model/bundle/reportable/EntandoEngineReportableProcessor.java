/**
 * this interface has to be implemented by those processors we want they create a report in order to manage
 * conflicts
 *
 * TARGETING ENTANDO ENGINE
 */
package org.entando.kubernetes.model.bundle.reportable;

public interface EntandoEngineReportableProcessor extends ReportableComponentProcessor {

    @Override
    default ReportableRemoteHandler getReportableRemoteHandler() {
        return ReportableRemoteHandler.ENTANDO_ENGINE;
    }
}