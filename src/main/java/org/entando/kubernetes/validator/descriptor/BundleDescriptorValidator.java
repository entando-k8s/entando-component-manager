package org.entando.kubernetes.validator.descriptor;

import java.util.LinkedHashMap;
import java.util.List;
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
        setupValidatorConfigurationDescriptorV6();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        var objectsThatMustNOTBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        objectsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);
        objectsThatMustNOTBeNull.put("code", BundleDescriptor::getCode);

        var objectsThatMustBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        objectsThatMustBeNull.put("name", BundleDescriptor::getName);

        addValidationConfigMap(DescriptorVersion.V1,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV5() {
        var objectsThatMustNOTBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        objectsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);
        objectsThatMustNOTBeNull.put("name", BundleDescriptor::getName);

        var objectsThatMustBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        objectsThatMustBeNull.put("code", BundleDescriptor::getCode);

        addValidationConfigMap(DescriptorVersion.V5,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV6() {
        var fieldsThatMustNOTBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        fieldsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);
        fieldsThatMustNOTBeNull.put("name", BundleDescriptor::getName);

        var fieldsThatMustBeNull = new LinkedHashMap<String, Function<BundleDescriptor, Object>>();
        fieldsThatMustBeNull.put("code", BundleDescriptor::getCode);

        addValidationConfigMap(DescriptorVersion.V6,
                List.of(super::validateDescriptorFormatOrThrow),
                fieldsThatMustNOTBeNull,
                fieldsThatMustBeNull);
    }

}
