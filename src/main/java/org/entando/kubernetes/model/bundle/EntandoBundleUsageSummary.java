package org.entando.kubernetes.model.bundle;

import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

public class EntandoBundleUsageSummary {

    public Map<ComponentType, Integer> componentUsage;

    public EntandoBundleUsageSummary() {
       componentUsage = new HashMap<>();
    }

    public EntandoBundleUsageSummary setComponentUsage(ComponentType type, int numberOfUsaged) {
        this.componentUsage.put(type, numberOfUsaged);
        return this;
    }

    public int getComponentUsage(ComponentType type) {
        return this.componentUsage.getOrDefault(type, 0);
    }
}
