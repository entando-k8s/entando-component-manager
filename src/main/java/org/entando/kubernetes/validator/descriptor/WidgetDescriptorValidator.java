package org.entando.kubernetes.validator.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptorVersion;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
public class WidgetDescriptorValidator extends BaseDescriptorValidator<WidgetDescriptor, WidgetDescriptorVersion> {

    public WidgetDescriptorValidator() {
        super(WidgetDescriptorVersion.class);
        super.validationConfigMap = new EnumMap<>(WidgetDescriptorVersion.class);
    }

    @Override
    protected WidgetDescriptorVersion readDescriptorVersion(WidgetDescriptor descriptor) {
        return WidgetDescriptorVersion.fromVersion(descriptor.getDescriptorVersion());
    }

    /**************************************************************************************************************
     * CONFIGURATION START.
     *************************************************************************************************************/

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV2();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("apiClaims", WidgetDescriptor::getApiClaims);
        objectsThatMustBeNull.put("configWidget", WidgetDescriptor::getConfigWidget);
        objectsThatMustBeNull.put("customElement", WidgetDescriptor::getCustomElement);

        configureValidationConfigMap(WidgetDescriptorVersion.V1,
                Collections.singletonList(super::validateDescriptorFormatOrThrow),
                getObjectsThatMustNotBeNullForEveryVersion(), objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV2() {

        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("configUi", WidgetDescriptor::getConfigUi);
        objectsThatMustBeNull.put("customUi", WidgetDescriptor::getCustomUi);
        objectsThatMustBeNull.put("customUiPath", WidgetDescriptor::getCustomUiPath);

        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustNotBeNullForEveryVersion
                = getObjectsThatMustNotBeNullForEveryVersion();
        objectsThatMustNotBeNullForEveryVersion.put("customElement", WidgetDescriptor::getCustomElement);

        configureValidationConfigMap(WidgetDescriptorVersion.V5,
                Arrays.asList(super::validateDescriptorFormatOrThrow, this::validateApiClaims),
                objectsThatMustNotBeNullForEveryVersion, objectsThatMustBeNull);
    }

    private Map<String, Function<WidgetDescriptor, Object>> getObjectsThatMustNotBeNullForEveryVersion() {
        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("code", WidgetDescriptor::getCode);
        objectsThatMustNOTBeNull.put("titles", WidgetDescriptor::getTitles);
        objectsThatMustNOTBeNull.put("group", WidgetDescriptor::getGroup);
        return objectsThatMustNOTBeNull;
    }

    /**
     * ensure consistency between the parsed API claims.
     * @param descriptor the widget descriptor to validate
     * @return the validated widget descriptor
     */
    private WidgetDescriptor validateApiClaims(WidgetDescriptor descriptor) {
        Optional.ofNullable(descriptor.getApiClaims()).orElseGet(ArrayList::new)
                .forEach(apiClaim -> {
                    if (apiClaim.getType().equals(WidgetDescriptor.ApiClaim.INTERNAL_API)
                            && !ObjectUtils.isEmpty(apiClaim.getBundleId())) {
                        throw new InvalidBundleException(
                                String.format(INTERNAL_API_CLAIM_WITH_BUNDLE_ID, descriptor.getCode()));
                    }
                    if (apiClaim.getType().equals(WidgetDescriptor.ApiClaim.EXTERNAL_API)
                            && ObjectUtils.isEmpty(apiClaim.getBundleId())) {
                        throw new InvalidBundleException(
                                String.format(EXTERNAL_API_CLAIM_WITHOUT_BUNDLE_ID, descriptor.getCode()));
                    }
                });

        return descriptor;
    }

    /**************************************************************************************************************
     * CONFIGURATION END.
     *************************************************************************************************************/

    public static final String INTERNAL_API_CLAIM_WITH_BUNDLE_ID =
            "The %s descriptor contains an API claim marked as internal but referencing an external bundle";
    public static final String EXTERNAL_API_CLAIM_WITHOUT_BUNDLE_ID =
            "The %s descriptor contains an API claim marked as external NOT referencing any external bundle";
}
