package org.entando.kubernetes.model.bundle.reportable;

import java.util.List;
import lombok.Value;
import org.entando.kubernetes.model.bundle.ComponentType;

@Value
public class Reportable {

    ComponentType componentType;
    List<String> codes;
    ReportableRemoteHandler reportableRemoteHandler;
}
