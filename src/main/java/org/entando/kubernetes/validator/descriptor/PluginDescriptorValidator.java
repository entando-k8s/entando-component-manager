package org.entando.kubernetes.validator.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PluginDescriptorValidator extends BaseDescriptorValidator<PluginDescriptor, PluginDescriptorVersion> {


    public static final String DNS_LABEL_HOST_REGEX = "^([a-z0-9][a-z0-9\\\\-]*[a-z0-9])$";
    public static final Pattern DNS_LABEL_HOST_REGEX_PATTERN = Pattern.compile(DNS_LABEL_HOST_REGEX);
    public static final String DESC_PROP_ENV_VARS = "environmentVariables";
    public static final int MIN_FULL_DEPLOYMENT_NAME_LENGTH = 50;
    public static final int MAX_FULL_DEPLOYMENT_NAME_LENGTH = 200;
    public static final int STANDARD_FULL_DEPLOYMENT_NAME_LENGTH = MAX_FULL_DEPLOYMENT_NAME_LENGTH;

    private final int fullDeploymentNameMaxlength;

    public PluginDescriptorValidator(@Value("${full.deployment.name.maxlength:" + STANDARD_FULL_DEPLOYMENT_NAME_LENGTH
            + "}") int fullDeploymentNameMaxlength) {

        super(PluginDescriptorVersion.class);
        super.validationConfigMap = new EnumMap<>(PluginDescriptorVersion.class);

        if (fullDeploymentNameMaxlength < MIN_FULL_DEPLOYMENT_NAME_LENGTH
                || fullDeploymentNameMaxlength > MAX_FULL_DEPLOYMENT_NAME_LENGTH) {

            throw new EntandoComponentManagerException(String.format(
                    "Wrong value received for the environment variable FULL_DEPLOYMENT_NAME_MAXLENGTH. "
                            + "Allowed value must be >= %d <= %d",
                    MIN_FULL_DEPLOYMENT_NAME_LENGTH, MAX_FULL_DEPLOYMENT_NAME_LENGTH));
        }
        this.fullDeploymentNameMaxlength = fullDeploymentNameMaxlength;
    }

    public int getFullDeploymentNameMaxlength() {
        return fullDeploymentNameMaxlength;
    }

    @Override
    protected PluginDescriptorVersion readDescriptorVersion(PluginDescriptor descriptor) {
        return PluginDescriptorVersion.fromVersion(descriptor.getDescriptorVersion());
    }

    /**************************************************************************************************************
     * CONFIGURATION START.
     *************************************************************************************************************/

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV2();
        setupValidatorConfigurationDescriptorV3();
        setupValidatorConfigurationDescriptorV4();
    }

    private void setupValidatorConfigurationDescriptorV1() {
        Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("spec", PluginDescriptor::getSpec);
        objectsThatMustNOTBeNull.put("spec.image", (PluginDescriptor descriptor) -> descriptor.getSpec().getImage());
        objectsThatMustNOTBeNull.put("spec.dbms", (PluginDescriptor descriptor) -> descriptor.getSpec().getDbms());
        objectsThatMustNOTBeNull.put("spec.healthCheckPath",
                (PluginDescriptor descriptor) -> descriptor.getSpec().getHealthCheckPath());

        Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("image", PluginDescriptor::getImage);
        objectsThatMustBeNull.put("dbms", PluginDescriptor::getDbms);
        objectsThatMustBeNull.put("deploymentBaseName", PluginDescriptor::getDeploymentBaseName);
        objectsThatMustBeNull.put("healthCheckPath", PluginDescriptor::getHealthCheckPath);
        objectsThatMustBeNull.put("roles", PluginDescriptor::getRoles);
        objectsThatMustBeNull.put("ingressPath", PluginDescriptor::getIngressPath);
        objectsThatMustBeNull.put("permissions", PluginDescriptor::getPermissions);
        objectsThatMustBeNull.put("securityLevel", PluginDescriptor::getSecurityLevel);
        objectsThatMustBeNull.put(DESC_PROP_ENV_VARS, PluginDescriptor::getEnvironmentVariables);

        configureValidationConfigMap(PluginDescriptorVersion.V1,
                Arrays.asList(
                        super::validateDescriptorFormatOrThrow,
                        this::validateSecurityLevelOrThrow,
                        this::validateFullDeploymentNameLength),
                objectsThatMustNOTBeNull, objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV2() {
        setupValidatorConfigurationDescriptorV2Onwards(PluginDescriptorVersion.V2);
    }

    private void setupValidatorConfigurationDescriptorV3() {
        setupValidatorConfigurationDescriptorV2Onwards(PluginDescriptorVersion.V3);
    }

    private void setupValidatorConfigurationDescriptorV4() {
        setupValidatorConfigurationDescriptorV2Onwards(PluginDescriptorVersion.V4);
        DescriptorValidatorConfigBean<PluginDescriptor, PluginDescriptorVersion> configBeanV4 = validationConfigMap.get(
                PluginDescriptorVersion.V4);
        configBeanV4.getObjectsThatMustBeNull().remove(DESC_PROP_ENV_VARS);
        configBeanV4.getValidationFunctions().add(this::validateEnvVarsOrThrow);
    }

    private void setupValidatorConfigurationDescriptorV2Onwards(PluginDescriptorVersion descriptorVersion) {
        Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("image", PluginDescriptor::getImage);
        objectsThatMustNOTBeNull.put("dbms", PluginDescriptor::getDbms);
        objectsThatMustNOTBeNull.put("healthCheckPath", PluginDescriptor::getHealthCheckPath);

        Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("spec", PluginDescriptor::getSpec);
        objectsThatMustBeNull.put(DESC_PROP_ENV_VARS, PluginDescriptor::getEnvironmentVariables);

        List<DescriptorValidationFunction<PluginDescriptor>> validationFunctionList = new ArrayList<>();
        validationFunctionList.add(this::validateDescriptorFormatOrThrow);
        validationFunctionList.add(this::validateSecurityLevelOrThrow);
        validationFunctionList.add(this::validateFullDeploymentNameLength);

        configureValidationConfigMap(descriptorVersion, validationFunctionList, objectsThatMustNOTBeNull,
                objectsThatMustBeNull);
    }

    /**************************************************************************************************************
     * CONFIGURATION END
     *************************************************************************************************************/

    /**
     * ensure that a plugin descriptor version is set. one and to set it
     *
     * @param descriptor the PluginDescriptor to analyze and setup
     * @return the received PluginDescriptor
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    @Override
    protected PluginDescriptor ensureDescriptorVersionIsSet(PluginDescriptor descriptor) {
        if (StringUtils.isEmpty(descriptor.getDescriptorVersion())) {
            if (descriptor.isVersion1()) {
                descriptor.setDescriptorVersion(PluginDescriptorVersion.V1.getVersion());
            } else {
                descriptor.setDescriptorVersion(PluginDescriptorVersion.V2.getVersion());
            }
        }

        return descriptor;
    }


    /**
     * validate the securityLevel of the plugin descriptor.
     *
     * @param descriptor the plugin descriptor to validate
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    private PluginDescriptor validateSecurityLevelOrThrow(PluginDescriptor descriptor) {

        if (!StringUtils.isEmpty(descriptor.getSecurityLevel())
                || (descriptor.isVersion1() && !StringUtils.isEmpty(descriptor.getSpec().getSecurityLevel()))) {

            String securityLevel =
                    descriptor.isVersion1() ? descriptor.getSpec().getSecurityLevel() : descriptor.getSecurityLevel();

            Arrays.stream(PluginSecurityLevel.values())
                    .filter(pluginSecurityLevel -> pluginSecurityLevel.toName().equals(securityLevel))
                    .findFirst()
                    .orElseThrow(() -> new InvalidBundleException(SECURITY_LEVEL_NOT_RECOGNIZED)); // NOSONAR
        }
        return descriptor;
    }

    /**
     * validate the environment variables of the plugin descriptor.
     *
     * @param descriptor the plugin descriptor to validate
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    private PluginDescriptor validateEnvVarsOrThrow(PluginDescriptor descriptor) {

        final List<EnvironmentVariable> environmentVariables =
                Optional.ofNullable(descriptor.getEnvironmentVariables())
                        .orElseGet(ArrayList::new);

        IntStream.range(0, environmentVariables.size()).forEach(i -> {
            final var envVar = environmentVariables.get(i);
            final var componentKey = descriptor.getComponentKey().getKey();
            final var varSecretRefKey = envVar.safeGetValueFrom().getSecretKeyRef();
            final var varName = envVar.getName();
            final var bundleId = descriptor.getDescriptorMetadata().getBundleId();

            // env var name empty
            if (ObjectUtils.isEmpty(varName)) {
                throw new InvalidBundleException(
                        String.format("In descriptor of plugin \"%s\" the environment var #%d has empty name",
                                componentKey, i + 1)
                );
            }

            if (varSecretRefKey != null) {

                // WITH SECRET REF

                // .. but the secret ref key is not valid
                if (!isSecretKeyRefValid(varSecretRefKey)) {
                    throw new InvalidBundleException(
                            String.format("Environment var \"%s\" of plugin \"%s\" contains an invalid reference",
                                    varName, componentKey)
                    );
                }

                // does secret belong to the bundle?
                if (!doesSecretBelongToTheBundle(bundleId, varSecretRefKey)) {
                    throw new InvalidBundleException(String.format(NON_OWNED_SECRET, componentKey, bundleId, varName));
                }
            }
        });

        return descriptor;
    }

    /**
     * receive a SecretKeyRef and check that it belongs to the owner plugin the rule to check is that the secret name
     * must end with the bundleId of the plugin.
     *
     * @param bundleId     the id of the bundle owning the current plugin
     * @param secretKeyRef the SecretKeyRef to validate
     * @return the validated SecretKeyRef
     */
    private boolean doesSecretBelongToTheBundle(String bundleId, SecretKeyRef secretKeyRef) {
        return secretKeyRef != null
                && (secretKeyRef.getName() == null
                        || secretKeyRef.getName().isEmpty()
                        || secretKeyRef.getName().startsWith(BundleUtilities.makeKubernetesCompatible(bundleId)));
    }

    /**
     * receive a SecretKeyRef and check that it's compliant with the required formats.
     *
     * @param secretKeyRef the SecretKeyRef to validate
     * @return the validated SecretKeyRef
     */
    private boolean isSecretKeyRefValid(SecretKeyRef secretKeyRef) {

        return ObjectUtils.isEmpty(secretKeyRef.getName())
                || (secretKeyRef.getName().length() <= BundleUtilities.GENERIC_K8S_ENTITY_MAX_LENGTH
                && DNS_LABEL_HOST_REGEX_PATTERN.matcher(secretKeyRef.getName()).matches());
    }


    /**
     * validate the fullDeploymentName. if the validation fails an EntandoComponentManagerException is thrown
     *
     * @param descriptor the descriptor of which validate the fullDeploymentName length
     * @return the validated string
     */
    private PluginDescriptor validateFullDeploymentNameLength(PluginDescriptor descriptor) {
        if (descriptor.getDescriptorMetadata().getFullDeploymentName().length() > fullDeploymentNameMaxlength) {

            throw new EntandoComponentManagerException(
                    String.format(
                            DEPLOYMENT_BASE_NAME_MAX_LENGTH_EXCEEDED_ERROR,
                            descriptor.getDescriptorMetadata().getFullDeploymentName(),
                            fullDeploymentNameMaxlength));
        }

        return descriptor;
    }


    public static final String SECURITY_LEVEL_NOT_RECOGNIZED =
            "The received plugin descriptor contains an unknown securityLevel. Accepted values are: "
                    + Arrays.stream(PluginSecurityLevel.values()).map(PluginSecurityLevel::toName)
                    .collect(Collectors.joining(", "));
    public static final String NON_OWNED_SECRET = ""
            + "The descriptor of the plugin \"%s\" of the bundle \"%s\" contains an invalid environment variable \"%s\""
            + " that points to a secret that doesn't belong to the plugin. Check documentation for details about "
            + "bundles secrets.";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGTH_EXCEEDED_ERROR =
            "The plugin full deployment name \"%s\" "
                    + "exceeds the max allowed length %d. You can configure the max length by setting the desired value of the "
                    + "environment variable FULL_DEPLOYMENT_NAME_MAXLENGTH";

}
