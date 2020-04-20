package org.entando.kubernetes.model.bundle;

import java.util.HashSet;
import java.util.Set;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;

public class EntandoBundleUsageSummary {

    public Set<EntandoCoreComponentUsage> componentUsage;

    public EntandoBundleUsageSummary() {
        componentUsage = new HashSet<>();
    }

    public EntandoBundleUsageSummary addComponentUsage(ComponentType type, String componentCode, int usageNumber) {
        this.componentUsage.add(new EntandoCoreComponentUsage(type.getTypeName(), componentCode, usageNumber));
        return this;
    }

    public EntandoBundleUsageSummary addComponentUsage(EntandoCoreComponentUsage usage) {
        this.componentUsage.add(usage);
        return this;
    }

    public int getComponentUsage(String componentType, String componentCode) {
        return this.componentUsage.stream()
                .filter(cu -> cu.getType().equals(componentType) && cu.getCode().equals(componentCode))
                .findFirst()
                .map(EntandoCoreComponentUsage::getUsage)
                .orElse(0);
    }

    public Set<EntandoCoreComponentUsage> getSummary() {
        return this.componentUsage;
    }
}
