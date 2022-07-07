package org.entando.kubernetes.validator.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
public class WidgetDescriptorValidator extends BaseDescriptorValidator<WidgetDescriptor> {

    private static final String BUNDLE_CODE_PARENT_REGEX = "^[\\w|-]+$";
    private static final Pattern BUNDLE_PARENT_CODE_REGEX = Pattern.compile(BUNDLE_CODE_PARENT_REGEX);
    public static final String FORMAT_MISSING_FIELD_FROM_WIDGET_TYPE = "In WidgetDescriptor \"%s\" "
            + "of version \"%s\" and widgetType \"%s\", field \"%s\" is required%s";

    /**************************************************************************************************************
     * CONFIGURATION START.
     *************************************************************************************************************/

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV5();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("apiClaims", WidgetDescriptor::getApiClaims);
        objectsThatMustBeNull.put("configWidget", WidgetDescriptor::getConfigWidget);
        objectsThatMustBeNull.put("customElement", WidgetDescriptor::getCustomElement);
        objectsThatMustBeNull.put("name", WidgetDescriptor::getName);
        objectsThatMustBeNull.put("parentName", WidgetDescriptor::getParentName);
        objectsThatMustBeNull.put("parentCode", WidgetDescriptor::getParentCode);

        final Map<String, Function<WidgetDescriptor, Object>> objectsThatMustNotBeNull
                = getObjectsThatMustNotBeNullForEveryVersion();
        objectsThatMustNotBeNull.put("code", WidgetDescriptor::getCode);
        objectsThatMustNotBeNull.put("titles", WidgetDescriptor::getTitles);

        addValidationConfigMap(DescriptorVersion.V1,
                Collections.singletonList(super::validateDescriptorFormatOrThrow),
                objectsThatMustNotBeNull, objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV5() {

        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("code", WidgetDescriptor::getCode);
        objectsThatMustBeNull.put("configUi", WidgetDescriptor::getConfigUi);

        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustNotBeNull
                = getObjectsThatMustNotBeNullForEveryVersion();

        objectsThatMustNotBeNull.put("name", WidgetDescriptor::getName);
        objectsThatMustNotBeNull.put("type", WidgetDescriptor::getType);

        List<DescriptorValidationFunction<WidgetDescriptor>> validationFunctionList = Arrays.asList(
                super::validateDescriptorFormatOrThrow, this::validateApiClaims,
                this::validateParentNameAndParentCode, this::dynamicValidationsForV5);

        addValidationConfigMap(DescriptorVersion.V5,
                validationFunctionList, objectsThatMustNotBeNull, objectsThatMustBeNull);
    }

    private WidgetDescriptor dynamicValidationsForV5(WidgetDescriptor widgetDescriptor) {
        switch (widgetDescriptor.getType()) {
            case WidgetDescriptor.TYPE_WIDGET_STANDARD:
                if (widgetDescriptor.getTitles() == null) {
                    mustHaveField(widgetDescriptor, "titles", widgetDescriptor.getTitles(), null);
                }

                if (widgetDescriptor.getCustomUi() == null && widgetDescriptor.getCustomUiPath() == null) {
                    mustHaveField(
                            widgetDescriptor,
                            "customElement",
                            widgetDescriptor.getCustomElement(),
                            "if no configUi or configUiPath is provided"
                    );
                }
                break;
            case WidgetDescriptor.TYPE_WIDGET_CONFIG:
            case WidgetDescriptor.TYPE_WIDGET_APPBUILDER:
                break;
            default:
                throw new IllegalArgumentException("unexpected widgetType \"" + widgetDescriptor.getType() + "\"");
        }
        return widgetDescriptor;
    }

    private void mustHaveField(WidgetDescriptor descriptor, String field, Object value, String comment) {
        //~
        if (value == null) {
            comment = (comment == null) ? "" : comment + " ";
            throw new InvalidBundleException(String.format(
                    FORMAT_MISSING_FIELD_FROM_WIDGET_TYPE,
                    descriptor.getComponentKey().getKey(),
                    descriptor.getDescriptorVersion(),
                    descriptor.getType(),
                    field,
                    comment));
        }
    }

    private Map<String, Function<WidgetDescriptor, Object>> getObjectsThatMustNotBeNullForEveryVersion() {
        Map<String, Function<WidgetDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
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

    /**
     * ensure consistency between parentName and parentCode fields.
     *
     * @param descriptor the widget descriptor to validate
     * @return the validated widget descriptor
     */
    private WidgetDescriptor validateParentNameAndParentCode(WidgetDescriptor descriptor) {
        String parentCode = descriptor.getParentCode();

        // mutual exclusion
        if (!ObjectUtils.isEmpty(descriptor.getParentName()) && !ObjectUtils.isEmpty(parentCode)) {

            throw new InvalidBundleException(
                    String.format(PARENT_NAME_AND_PARENT_CODE_BOTH_PRESENT, descriptor.getCode()));
        }

        // check the format
        if (!ObjectUtils.isEmpty(parentCode)) {
            if (!BUNDLE_PARENT_CODE_REGEX.matcher(parentCode).matches()) {
                throw new InvalidBundleException(String.format(WRONG_PARENT_CODE_FORMAT, descriptor.getCode()));
            }
        }

        return descriptor;
    }

    /**************************************************************************************************************
     * CONFIGURATION END.
     *************************************************************************************************************/

    public static final String INTERNAL_API_CLAIM_WITH_BUNDLE_ID =
            "The %s descriptor contains an API claim marked as internal but referencing an external bundle";
    public static final String EXTERNAL_API_CLAIM_WITHOUT_BUNDLE_ID =
            "The %s descriptor contains an API claim marked as external NOT referencing any external bundle";
    public static final String PARENT_NAME_AND_PARENT_CODE_BOTH_PRESENT =
            "The %s descriptor contains both a parentName and a parentCode. They are mutually exclusive";
    public static final String WRONG_PARENT_CODE_FORMAT =
            "The %s descriptor contains a parentCode that not respects the format " + BUNDLE_CODE_PARENT_REGEX;
    public static final String FTL_NOT_AVAILABLE =
            "The %s descriptor does NOT contains any FTL. In widget descriptor v1 one of \"customUi\" and "
                    + "\"customUiPath\" must be populated";
}
