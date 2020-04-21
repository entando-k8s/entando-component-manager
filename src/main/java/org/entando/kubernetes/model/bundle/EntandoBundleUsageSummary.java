package org.entando.kubernetes.model.bundle;

import java.util.ArrayList;
import java.util.List;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;

public class EntandoBundleUsageSummary {

    private final List<EntandoCoreComponentUsage> componentUsage;

    public EntandoBundleUsageSummary() {
        componentUsage = new ArrayList<>();
    }

    public EntandoBundleUsageSummary addComponentUsage(ComponentType type, String componentCode, int usageNumber) {
        if (this.componentUsage.stream().noneMatch(cu -> cu.getCode().equals(componentCode) && cu.getType().equals(type.getTypeName()))) {
            this.componentUsage.add(new EntandoCoreComponentUsage(type.getTypeName(), componentCode, usageNumber));
        }
        return this;
    }

    public EntandoBundleUsageSummary addComponentUsage(EntandoCoreComponentUsage usage) {
        if (this.componentUsage.stream().noneMatch(cu -> cu.getCode().equals(usage.getCode()) && cu.getType().equals(usage.getType()))) {
            this.componentUsage.add(usage);
        }
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
        return new ArrayList<>(this.componentUsage);
    }
}
