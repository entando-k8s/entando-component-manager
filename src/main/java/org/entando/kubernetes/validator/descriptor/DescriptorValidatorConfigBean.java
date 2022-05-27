/**
 * generic bean to bind descriptor versions and some validation constraints.
 * D stands for Descriptor and defines the descriptor class
 * V stands for DescriptorVersion and defines the class to use as accepted descriptor versions list
 */

package org.entando.kubernetes.validator.descriptor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Accessors(chain = true)
public class DescriptorValidatorConfigBean<D, V> {

    private V pluginDescriptorVersion;
    private List<DescriptorValidationFunction<D>> validationFunctions;
    private Map<String, Function<D, Object>> objectsThatMustNOTBeNull;
    private Map<String, Function<D, Object>> objectsThatMustBeNull;

    @FunctionalInterface
    interface DescriptorValidationFunction<D> {

        /**
         * apply a validation to the received PluginDescriptor.
         *
         * @param descriptor the PluginDescriptor to validate
         * @return the received and validated PluginDescriptor
         * @throws InvalidBundleException if the PluginDescriptor has not been successfully validated
         */
        D validateOrThrow(D descriptor) throws InvalidBundleException;
    }
}
