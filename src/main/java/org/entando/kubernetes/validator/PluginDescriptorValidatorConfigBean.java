package org.entando.kubernetes.validator;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
import org.entando.kubernetes.validator.PluginDescriptorValidator.PluginDescriptorValidationFunction;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Accessors(chain = true)
public class PluginDescriptorValidatorConfigBean {

    private PluginDescriptorVersion pluginDescriptorVersion;
    private List<PluginDescriptorValidationFunction> validationFunctions;
    private Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull;
    private Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull;
}
