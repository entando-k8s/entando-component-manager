package org.entando.kubernetes.service.digitalexchange.component;

import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantEntandoCoreComponentUsage;
import org.springframework.stereotype.Service;

@Service
public class EntandoBundleComponentUsageService {

    private final EntandoCoreClient client;

    public EntandoBundleComponentUsageService(EntandoCoreClient client) {
        this.client = client;
    }

    public EntandoCoreComponentUsage getUsage(ComponentType componentType, String componentCode) {
        switch (componentType) {
            case WIDGET:
                return this.client.getWidgetUsage(componentCode);
            case GUI_FRAGMENT:
                return this.client.getFragmentUsage(componentCode);
            case PAGE:
                return this.client.getPageUsage(componentCode);
            case PAGE_MODEL:
                return this.client.getPageModelUsage(componentCode);
            case CONTENT_TYPE:
                return this.client.getContentTypeUsage(componentCode);
            default:
                return new IrrelevantEntandoCoreComponentUsage(componentCode);
        }
    }
}
