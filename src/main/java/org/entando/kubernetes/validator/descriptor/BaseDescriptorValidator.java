package org.entando.kubernetes.validator.descriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class BaseDescriptorValidator<D extends VersionedDescriptor, V extends DescriptorVersion> {

    private final Class<? extends Enum<?>> descriptorVersionEnum;
    protected Map<V, DescriptorValidatorConfigBean<D, V>> validationConfigMap;

    /**
     * returns the DescriptorVersion of the received VersionedDescriptor.
     *
     * @param descriptor the VersionedDescriptor of which return the current DescriptorVersion
     * @return the DescriptorVersion of the received VersionedDescriptor
     */
    protected abstract V readDescriptorVersion(D descriptor);

    /**
     * ensure that the received descriptor version is set. if the descriptor version is not set, set it.
     *
     * @param descriptor the Descriptor to analyze and setup
     * @return the received Descriptor
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    protected D ensureDescriptorVersionIsSet(D descriptor) {
        if (StringUtils.isEmpty(descriptor.getDescriptorVersion())) {
            if (descriptor.isVersion1()) {
                descriptor.setDescriptorVersion(DescriptorVersion.V1);
            }
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
    public void configureValidationConfigMap(V descriptorVersion,
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
            final V descriptorVersion = getDescriptorVersionOrThrow(descriptor);
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

        final V descriptorVersion = readDescriptorVersion(descriptor);

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
    private V getDescriptorVersionOrThrow(D descriptor) {
        final V descriptorVersion = readDescriptorVersion(descriptor);

        if (descriptorVersion == null) {
            String error = String.format(VERSION_NOT_VALID, descriptor.getComponentKey().getKey(),
                    Arrays.stream(descriptorVersionEnum.getEnumConstants())
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
    private void validateNullAndNonNullObjects(V descriptorVersion,
            D descriptor,
            Map<String, Function<D, Object>> objectsThatMustNOTBeNull,
            Map<String, Function<D, Object>> objectsThatMustBeNull) {

        objectsThatMustNOTBeNull.forEach((key, fn) -> {
            if (fn.apply(descriptor) == null) {
                throw new InvalidBundleException(String.format(EXPECTED_NOT_NULL_IS_NULL,
                        descriptorVersion.getVersion(), key));
            }
        });

        objectsThatMustBeNull.forEach((key, fn) -> {
            if (fn.apply(descriptor) != null) {
                throw new InvalidBundleException(String.format(EXPECTED_NULL_IS_NOT_NULL,
                        descriptorVersion.getVersion(), key));
            }
        });
    }

    public static final String VERSION_NOT_VALID =
            "The %s descriptor contains an invalid descriptorVersion. Accepted versions are: %s";
    public static final String EXPECTED_NOT_NULL_IS_NULL =
            "Descriptor version detected: %s. With this version the %s property must NOT be null.";
    public static final String EXPECTED_NULL_IS_NOT_NULL =
            "Descriptor version detected: %s. With this version the %s property must be null.";

}
