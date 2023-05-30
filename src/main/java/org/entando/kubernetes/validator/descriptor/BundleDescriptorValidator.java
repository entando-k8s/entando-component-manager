package org.entando.kubernetes.validator.descriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
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
        var objectsThatMustNOTBeNull = createListFromPairs(
                new DefaultKeyValue<>("components", BundleDescriptor::getComponents),
                new DefaultKeyValue<>("code", BundleDescriptor::getCode)
        );

        var objectsThatMustBeNull = createListFromPairs(
                new DefaultKeyValue<>("name", BundleDescriptor::getName)
        );

        addValidationConfigMap(DescriptorVersion.V1,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV5() {
        var objectsThatMustNOTBeNull = createListFromPairs(
                new DefaultKeyValue<>("components", BundleDescriptor::getComponents),
                new DefaultKeyValue<>("name", BundleDescriptor::getName)
        );

        var objectsThatMustBeNull = createListFromPairs(
                new DefaultKeyValue<>("code", BundleDescriptor::getCode)
        );

        addValidationConfigMap(DescriptorVersion.V5,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV6() {
        var fieldsThatMustNOTBeNull = createListFromPairs(
                new DefaultKeyValue<>("components", BundleDescriptor::getComponents),
                new DefaultKeyValue<>("name", BundleDescriptor::getName)
        );

        var fieldsThatMustBeNull = createListFromPairs(
                new DefaultKeyValue<>("code", BundleDescriptor::getCode)
        );

        addValidationConfigMap(DescriptorVersion.V6,
                List.of(super::validateDescriptorFormatOrThrow),
                fieldsThatMustNOTBeNull,
                fieldsThatMustBeNull);
    }


    private Map<String, Function<BundleDescriptor, Object>> createListFromPairs(KeyValue<String, Function<BundleDescriptor, Object>>... pairs) {
        Map<String, Function<BundleDescriptor, Object>> list = new LinkedHashMap<>();
        for (KeyValue<String, Function<BundleDescriptor, Object>> pair : pairs) {
            String key = pair.getKey();
            Function<BundleDescriptor, Object> getter = pair.getValue();
            list.put(key, getter);
        }
        return list;
    }

}
