package org.entando.kubernetes.validator.descriptor;

import static org.springframework.util.ObjectUtils.isEmpty;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PageDescriptorValidator extends BaseDescriptorValidator<PageDescriptor> {

    private static final String CODE_REGEX = "^[\\w|-]+-[0-9a-fA-F]{8}$";
    private static final Pattern PATTERN_REGEX = Pattern.compile(CODE_REGEX);

    public static final String NAME_AND_CODE_BOTH_PRESENT
            = "The %s descriptor contains both a name and a code. They are mutually exclusive";
    public static final String PARENT_NAME_AND_PARENT_CODE_BOTH_PRESENT
            = "The %s descriptor contains both a parentName and a parentCode. They are mutually exclusive";
    public static final String NAME_OR_CODE_REQUIRED
            = "The %s descriptor has to contains name or code";
    public static final String PARENT_NAME_OR_PARENT_CODE_REQUIRED
            = "The %s descriptor has to contains parentName or parentCode";


    public static final String WRONG_FORMAT
            = "The %s descriptor contains a %s that not respects the format " + CODE_REGEX;

    /**
     * ************************************************************************************************************
     * CONFIGURATION START.
     ************************************************************************************************************
     */

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV5();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        Map<String, Function<PageDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("name", PageDescriptor::getName);
        objectsThatMustBeNull.put("parentName", PageDescriptor::getParentName);

        final Map<String, Function<PageDescriptor, Object>> objectsThatMustNotBeNull
                = getObjectsThatMustNotBeNullForEveryVersion();
        objectsThatMustNotBeNull.put("code", PageDescriptor::getCode);
        objectsThatMustNotBeNull.put("parentCode", PageDescriptor::getParentCode);

        addValidationConfigMap(DescriptorVersion.V1,
                Collections.singletonList(super::validateDescriptorFormatOrThrow),
                objectsThatMustNotBeNull, objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV5() {
        Map<String, Function<PageDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        Map<String, Function<PageDescriptor, Object>> objectsThatMustNotBeNull
                = this.getObjectsThatMustNotBeNullForEveryVersion();

        List<DescriptorValidationFunction<PageDescriptor>> validationFunctionList = Arrays.asList(
                super::validateDescriptorFormatOrThrow, this::validateNameAndCodeForV5, this::validateParentNameAndParentCodeForV5);

        addValidationConfigMap(DescriptorVersion.V5,
                validationFunctionList, objectsThatMustNotBeNull, objectsThatMustBeNull);
    }

    private Map<String, Function<PageDescriptor, Object>> getObjectsThatMustNotBeNullForEveryVersion() {
        Map<String, Function<PageDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("titles", PageDescriptor::getTitles);
        objectsThatMustNOTBeNull.put("pageModel", PageDescriptor::getPageModel);
        objectsThatMustNOTBeNull.put("ownerGroup", PageDescriptor::getOwnerGroup);
        return objectsThatMustNOTBeNull;
    }

    private PageDescriptor validateNameAndCodeForV5(PageDescriptor pageDescriptor) {
        String descriptorCodeForMessage = (StringUtils.isBlank(pageDescriptor.getCode()) ? pageDescriptor.getName() : pageDescriptor.getCode());
        if (!isEmpty(pageDescriptor.getCode()) && !isEmpty(pageDescriptor.getName())) {
            throw new InvalidBundleException(
                    String.format(NAME_AND_CODE_BOTH_PRESENT, descriptorCodeForMessage));
        } else if (isEmpty(pageDescriptor.getCode()) && isEmpty(pageDescriptor.getName())) {
            throw new InvalidBundleException(
                    String.format(NAME_OR_CODE_REQUIRED, descriptorCodeForMessage));
        }
        if (!isEmpty(pageDescriptor.getCode()) && !PATTERN_REGEX.matcher(pageDescriptor.getCode()).matches()) {
            throw new InvalidBundleException(String.format(WRONG_FORMAT, pageDescriptor.getCode(), "code"));
        }
        return pageDescriptor;
    }

    private PageDescriptor validateParentNameAndParentCodeForV5(PageDescriptor pageDescriptor) {
        String descriptorCodeForMessage = (StringUtils.isBlank(pageDescriptor.getCode()) ? pageDescriptor.getName() : pageDescriptor.getCode());
        if (!isEmpty(pageDescriptor.getParentCode()) && !isEmpty(pageDescriptor.getParentName())) {
            throw new InvalidBundleException(
                    String.format(PARENT_NAME_AND_PARENT_CODE_BOTH_PRESENT, descriptorCodeForMessage));
        } else if (isEmpty(pageDescriptor.getParentCode()) && isEmpty(pageDescriptor.getParentName())) {
            throw new InvalidBundleException(
                    String.format(PARENT_NAME_OR_PARENT_CODE_REQUIRED, descriptorCodeForMessage));
        }
        if (!isEmpty(pageDescriptor.getParentCode()) && !PATTERN_REGEX.matcher(pageDescriptor.getParentCode()).matches()) {
            throw new InvalidBundleException(String.format(WRONG_FORMAT, descriptorCodeForMessage, "parentCode"));
        }
        return pageDescriptor;
    }
    
    /**
     * ************************************************************************************************************
     * CONFIGURATION END.
     ************************************************************************************************************
     */
}
