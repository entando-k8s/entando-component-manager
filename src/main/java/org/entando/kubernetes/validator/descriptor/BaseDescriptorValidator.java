package org.entando.kubernetes.validator.descriptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class BaseDescriptorValidator<D extends VersionedDescriptor> {

    protected Map<DescriptorVersion, DescriptorValidatorConfigBean<D>> validationConfigMap = new EnumMap<>(
            DescriptorVersion.class);

    /**
     * returns the DescriptorVersion of the received VersionedDescriptor.
     *
     * @param descriptor the VersionedDescriptor of which return the current DescriptorVersion
     * @return the DescriptorVersion of the received VersionedDescriptor
     */
    protected DescriptorVersion readDescriptorVersion(D descriptor) {
        return DescriptorVersion.fromVersion(descriptor.getDescriptorVersion());
    }

    /**
     * ensure that the received descriptor version is set. if the descriptor version is not set, set it.
     *
     * @param descriptor the Descriptor to analyze and setup
     * @return the received Descriptor
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    public D ensureDescriptorVersionIsSet(D descriptor) {
        if (StringUtils.isEmpty(descriptor.getDescriptorVersion())) {
            descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        }

        return descriptor;
    }

    /**
     * configure the validation map.
     */
    abstract void setupValidatorConfiguration();


    /**
     * configure the ValidationConfigMap with the data required to validate the current descriptor.
     *
     * @param descriptorVersion        the DescriptorVersion identifying the validations to apply
     * @param validationFunctionList   the validation function list
     * @param objectsThatMustNOTBeNull the map of the object that must not be null in the descriptor
     * @param objectsThatMustBeNull    the map of the object that must be null in the descriptor
     */
    public void addValidationConfigMap(DescriptorVersion descriptorVersion,
            List<DescriptorValidationFunction<D>> validationFunctionList,
            Map<String, Function<D, Object>> objectsThatMustNOTBeNull,
            Map<String, Function<D, Object>> objectsThatMustBeNull) {

        validationConfigMap.put(descriptorVersion, new DescriptorValidatorConfigBean<>(
                descriptorVersion,
                validationFunctionList,
                objectsThatMustNOTBeNull,
                objectsThatMustBeNull
        ));
    }

    /**
     * validate the received VersionedDescriptor.
     *
     * @param descriptor the VersionedDescriptor to validate
     * @return true if the validation succeeds
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    public boolean validateOrThrow(D descriptor) {

        if (null != descriptor) {
            ensureDescriptorVersionIsSet(descriptor);
            final DescriptorVersion descriptorVersion = getDescriptorVersionOrThrow(descriptor);
            validationConfigMap.get(descriptorVersion).getValidationFunctions()
                    .forEach(validationFunction -> validationFunction.validateOrThrow(descriptor));
        }

        return true;
    }

    /**
     * validate the version of the VersionedDescriptor.
     *
     * @param descriptor the descriptor to validate
     * @return the validated VersionedDescriptor
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    protected D validateDescriptorFormatOrThrow(D descriptor) {

        final DescriptorVersion descriptorVersion = readDescriptorVersion(descriptor);

        validateNullAndNonNullObjects(
                descriptorVersion,
                descriptor,
                validationConfigMap.get(descriptorVersion).getObjectsThatMustNOTBeNull(),
                validationConfigMap.get(descriptorVersion).getObjectsThatMustBeNull());

        return descriptor;
    }

    /**
     * check if the version of the VersionedDescriptor is one of the expected ones.
     *
     * @param descriptor the VersionedDescriptor to validate
     * @return the DescriptorVersion detected
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    private DescriptorVersion getDescriptorVersionOrThrow(D descriptor) {
        final DescriptorVersion descriptorVersion = readDescriptorVersion(descriptor);

        if (descriptorVersion == null || ! validationConfigMap.containsKey(descriptorVersion)) {
            String error = String.format(VERSION_NOT_VALID, "\"" + descriptor.getComponentKey().getKey() + "\"",
                    descriptor.getDescriptorClassName(),
                    validationConfigMap.keySet().stream()
                            .map((Object t) -> ((DescriptorVersion) t).getVersion())
                            .collect(Collectors.joining(", ")));
            log.debug(error);
            throw new InvalidBundleException(error);
        }
        return descriptorVersion;
    }


    /**
     * check that the received maps of values are the expected ones.
     *
     * @param descriptorVersion        the DescriptorVersion read by the descriptor
     * @param descriptor               the VersionedDescriptor to validate
     * @param objectsThatMustNOTBeNull the map of supplier that must return a non null object
     * @param objectsThatMustBeNull    the map of supplier that must return a null object
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    private void validateNullAndNonNullObjects(DescriptorVersion descriptorVersion,
            D descriptor,
            Map<String, Function<D, Object>> objectsThatMustNOTBeNull,
            Map<String, Function<D, Object>> objectsThatMustBeNull) {

        objectsThatMustNOTBeNull.forEach((key, fn) -> {
            if (ObjectUtils.isEmpty(fn.apply(descriptor))) {
                throw new InvalidBundleException(String.format(EXPECTED_NOT_NULL_IS_NULL,
                        descriptor.getDescriptorClassName(),
                        descriptor.getComponentKey().getKey(),
                        descriptorVersion.getVersion(), key));
            }
        });

        objectsThatMustBeNull.forEach((key, fn) -> {
            if (ObjectUtils.isNotEmpty(fn.apply(descriptor))) {
                throw new InvalidBundleException(String.format(EXPECTED_NULL_IS_NOT_NULL,
                        descriptor.getDescriptorClassName(),
                        descriptor.getComponentKey().getKey(),
                        descriptorVersion.getVersion(), key));
            }
        });
    }

    public static final String VERSION_NOT_VALID =
            "The %s descriptor contains an invalid descriptorVersion. Accepted versions for %s are: %s";
    public static final String EXPECTED_NOT_NULL_IS_NULL =
            "%s descriptor %s version detected: %s. With this version the %s property must NOT be null.";
    public static final String EXPECTED_NULL_IS_NOT_NULL =
            "%s descriptor %s version detected: %s. With this version the %s property must be null.";

}
