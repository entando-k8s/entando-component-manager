package org.entando.kubernetes.validator.descriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BundleDescriptorValidator extends BaseDescriptorValidator<BundleDescriptor> {

    /**************************************************************************************************************
     * CONFIGURATION START.
     *************************************************************************************************************/

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV5();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        Map<String, Function<BundleDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);
        objectsThatMustNOTBeNull.put("code", BundleDescriptor::getCode);

        Map<String, Function<BundleDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("name", BundleDescriptor::getName);

        addValidationConfigMap(DescriptorVersion.V1,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV5() {
        Map<String, Function<BundleDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);
        objectsThatMustNOTBeNull.put("name", BundleDescriptor::getName);

        Map<String, Function<BundleDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("code", BundleDescriptor::getCode);

        addValidationConfigMap(DescriptorVersion.V5,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }
}
