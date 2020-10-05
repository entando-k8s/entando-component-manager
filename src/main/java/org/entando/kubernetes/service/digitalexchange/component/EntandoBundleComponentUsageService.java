package org.entando.kubernetes.service.digitalexchange.component;

import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
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
            case GROUP:
                return this.client.getGroupUsage(componentCode);
            case FRAGMENT:
                return this.client.getFragmentUsage(componentCode);
            case CATEGORY:
                return this.client.getCategoryUsage(componentCode);
            case PAGE:
                return this.client.getPageUsage(componentCode);
            case PAGE_TEMPLATE:
                return this.client.getPageModelUsage(componentCode);
            case CONTENT_TYPE:
                return this.client.getContentTypeUsage(componentCode);
            case CONTENT_TEMPLATE:
                return this.client.getContentModelUsage(componentCode);
            default:
                return new IrrelevantComponentUsage(componentCode);
        }
    }
}
