package org.entando.kubernetes.validator.descriptor;

import java.util.HashMap;
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
        Map<String, Function<BundleDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("code", BundleDescriptor::getCode);
        objectsThatMustNOTBeNull.put("components", BundleDescriptor::getComponents);

        addValidationConfigMap(DescriptorVersion.V1,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                new HashMap<>());

        addValidationConfigMap(DescriptorVersion.V5,
                List.of(super::validateDescriptorFormatOrThrow),
                objectsThatMustNOTBeNull,
                new HashMap<>());
    }
}
