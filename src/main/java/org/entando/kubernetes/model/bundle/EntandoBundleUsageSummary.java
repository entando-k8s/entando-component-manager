package org.entando.kubernetes.model.bundle;

import java.util.ArrayList;
import java.util.List;
import org.entando.kubernetes.model.entandocore.usage.EntandoCoreComponentUsage;

public class EntandoBundleUsageSummary {

    public List<EntandoCoreComponentUsage> componentUsage;

    public EntandoBundleUsageSummary() {
       componentUsage = new ArrayList<>();
    }

    public EntandoBundleUsageSummary addComponentUsage(String type, String componentCode, int usageNumber) {
        this.componentUsage.add(new EntandoCoreComponentUsage(type, componentCode, usageNumber));
        return this;
    }

    public int getComponentUsage(String componentType, String componentCode) {
        return this.componentUsage.stream()
                .filter(cu -> cu.getType().equals(componentType) && cu.getCode().equals(componentCode))
                .findFirst()
                .map(EntandoCoreComponentUsage::getUsage)
                .orElse(0);
    }

    public List<EntandoCoreComponentUsage> getSummary() {
        return this.componentUsage;
    }
}
