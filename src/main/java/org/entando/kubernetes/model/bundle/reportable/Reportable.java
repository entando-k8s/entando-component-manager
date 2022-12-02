package org.entando.kubernetes.model.bundle.reportable;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.entando.kubernetes.model.bundle.ComponentType;

@Value
public class Reportable {

    ComponentType componentType;
    List<Component> components;
    ReportableRemoteHandler reportableRemoteHandler;


    public Reportable(ComponentType componentType,
            ReportableRemoteHandler reportableRemoteHandler,
            List<Component> components) {

        this.componentType = componentType;
        this.reportableRemoteHandler = reportableRemoteHandler;
        this.components = components;
    }

    public Reportable(ComponentType componentType,
            List<String> componentsCodes,
            ReportableRemoteHandler reportableRemoteHandler) {

        this.componentType = componentType;
        this.reportableRemoteHandler = reportableRemoteHandler;
        this.components = componentsCodes.stream()
                .map(code -> new Component(code, ""))
                .collect(Collectors.toList());
    }

    public List<String> getCodes() {
        return components.stream().map(Component::getCode).collect(Collectors.toList());
    }

    @Value
    public static class Component {
        String code;
        String version;
    }
}
