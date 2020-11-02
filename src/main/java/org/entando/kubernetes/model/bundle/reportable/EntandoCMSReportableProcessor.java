/**
 * this interface has to be implemented by those processors we want they create a report in order to manage
 * conflicts
 *
 * TARGETING ENTANDO CMS
 */
package org.entando.kubernetes.model.bundle.reportable;

public interface EntandoCMSReportableProcessor extends ReportableComponentProcessor {

    @Override
    default ReportableRemoteHandler getReportableRemoteHandler() {
        return ReportableRemoteHandler.ENTANDO_CMS;
    }
}