package org.entando.kubernetes.model.bundle.processor;

import java.util.Map;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport.Status;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;

public abstract class BaseComponentProcessor<T extends Descriptor> implements ComponentProcessor<T> {

    protected InstallAction extractInstallAction(String componentCode, InstallActionsByComponentType actions,
            InstallAction conflictStrategy, AnalysisReport report) {

        Map<String, InstallAction> actionsByType = actions.getActionsByType(getSupportedComponentType());
        if (actionsByType.containsKey(componentCode)) {
            return actionsByType.get(componentCode);
        }

        if (isConflict(componentCode, report)) {
            return conflictStrategy;
        }

        return InstallAction.CREATE;
    }

    protected boolean isConflict(String contentId, AnalysisReport report) {
        Map<String, Status> reportByType = report.getReportByType(getSupportedComponentType());

        return reportByType.containsKey(contentId)
                && reportByType.get(contentId) != Status.NEW;
    }

}
