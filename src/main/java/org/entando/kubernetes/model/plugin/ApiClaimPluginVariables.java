package org.entando.kubernetes.model.plugin;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiClaimPluginVariables {

    private final String apiClaimName;
    private final List<PluginVariable> pluginVariableList;
}
