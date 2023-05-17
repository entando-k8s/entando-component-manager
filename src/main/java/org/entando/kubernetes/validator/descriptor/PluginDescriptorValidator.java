package org.entando.kubernetes.validator.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginResources;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PluginDescriptorValidator extends BaseDescriptorValidator<PluginDescriptor> {


    public static final String DNS_LABEL_HOST_REGEX = "^([a-z0-9][a-z0-9\\\\-]*[a-z0-9])$";
    public static final Pattern DNS_LABEL_HOST_REGEX_PATTERN = Pattern.compile(DNS_LABEL_HOST_REGEX);

    public static final String MEM_AND_STORAGE_REGEX = "^(\\d+)(G|M|K|Gi|Mi|Ki)$";
    public static final Pattern MEM_AND_STORAGE_PATTERN = Pattern.compile(MEM_AND_STORAGE_REGEX);

    public static final String CPU_REGEX = "^(\\d+)(m)$";
    public static final Pattern CPU_PATTERN = Pattern.compile(CPU_REGEX);
    public static final String DESC_PROP_ENV_VARS = "environmentVariables";
    public static final String DESC_PROP_RESOURCES = "resources";
    public static final int MIN_FULL_DEPLOYMENT_NAME_LENGTH = 50;
    public static final int MAX_FULL_DEPLOYMENT_NAME_LENGTH = 200;
    public static final int STANDARD_FULL_DEPLOYMENT_NAME_LENGTH = MAX_FULL_DEPLOYMENT_NAME_LENGTH;
    public static final int ROLES_MAX_LENGTH = 4000;

    private final int fullDeploymentNameMaxlength;

    public PluginDescriptorValidator(@Value("${full.deployment.name.maxlength:" + STANDARD_FULL_DEPLOYMENT_NAME_LENGTH
            + "}") int fullDeploymentNameMaxlength) {

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

    /**************************************************************************************************************
     * CONFIGURATION START.
     *************************************************************************************************************/

    @PostConstruct
    public void setupValidatorConfiguration() {
        setupValidatorConfigurationDescriptorV1();
        setupValidatorConfigurationDescriptorV2();
        setupValidatorConfigurationDescriptorV3();
        setupValidatorConfigurationDescriptorV4();
        setupValidatorConfigurationDescriptorV5();
        setupValidatorConfigurationDescriptorV6();
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
        objectsThatMustBeNull.put(DESC_PROP_RESOURCES, PluginDescriptor::getResources);

        addValidationConfigMap(DescriptorVersion.V1,
                Arrays.asList(
                        super::validateDescriptorFormatOrThrow,
                        this::validateSecurityLevelOrThrow,
                        this::validateFullDeploymentNameLength),
                objectsThatMustNOTBeNull, objectsThatMustBeNull);
    }

    private void setupValidatorConfigurationDescriptorV2() {
        setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion.V2);
    }

    private void setupValidatorConfigurationDescriptorV3() {
        setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion.V3);
    }

    private void setupValidatorConfigurationDescriptorV4() {
        setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion.V4);
        DescriptorValidatorConfigBean<PluginDescriptor> configBeanV4 = validationConfigMap.get(
                DescriptorVersion.V4);
        configBeanV4.getObjectsThatMustBeNull().remove(DESC_PROP_ENV_VARS);
        configBeanV4.getValidationFunctions().add(this::validateEnvVarsOrThrow);
    }

    private void setupValidatorConfigurationDescriptorV5() {
        setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion.V5);
        DescriptorValidatorConfigBean<PluginDescriptor> configBeanV5 = validationConfigMap.get(
                DescriptorVersion.V5);
        configBeanV5.getObjectsThatMustNOTBeNull().put("name", PluginDescriptor::getName);
        configBeanV5.getObjectsThatMustBeNull().remove(DESC_PROP_ENV_VARS);
        configBeanV5.getValidationFunctions().add(this::validateEnvVarsOrThrow);
    }

    private void setupValidatorConfigurationDescriptorV6() {
        setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion.V6);
        DescriptorValidatorConfigBean<PluginDescriptor> configBeanV6 = validationConfigMap.get(
                DescriptorVersion.V6);
        configBeanV6.getObjectsThatMustNOTBeNull().put("name", PluginDescriptor::getName);
        configBeanV6.getObjectsThatMustBeNull().remove(DESC_PROP_ENV_VARS);
        configBeanV6.getObjectsThatMustBeNull().remove(DESC_PROP_RESOURCES);
        configBeanV6.getValidationFunctions().add(this::validateEnvVarsOrThrow);
        configBeanV6.getValidationFunctions().add(this::validatePluginResources);
    }

    private void setupValidatorConfigurationDescriptorV2Onwards(DescriptorVersion descriptorVersion) {
        Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
        objectsThatMustNOTBeNull.put("image", PluginDescriptor::getImage);
        objectsThatMustNOTBeNull.put("dbms", PluginDescriptor::getDbms);
        objectsThatMustNOTBeNull.put("healthCheckPath", PluginDescriptor::getHealthCheckPath);

        Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
        objectsThatMustBeNull.put("spec", PluginDescriptor::getSpec);
        objectsThatMustBeNull.put(DESC_PROP_ENV_VARS, PluginDescriptor::getEnvironmentVariables);
        objectsThatMustBeNull.put(DESC_PROP_RESOURCES, PluginDescriptor::getResources);

        List<DescriptorValidationFunction<PluginDescriptor>> validationFunctionList = new ArrayList<>();
        validationFunctionList.add(this::validateDescriptorFormatOrThrow);
        validationFunctionList.add(this::validateCustomIngressPathOrThrow);
        validationFunctionList.add(this::validateRolesLengthOrThrow);
        validationFunctionList.add(this::validateSecurityLevelOrThrow);
        validationFunctionList.add(this::validateFullDeploymentNameLength);

        addValidationConfigMap(descriptorVersion, validationFunctionList, objectsThatMustNOTBeNull,
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
    public PluginDescriptor ensureDescriptorVersionIsSet(PluginDescriptor descriptor) {
        if (StringUtils.isEmpty(descriptor.getDescriptorVersion())) {
            if (descriptor.isVersion1()) {
                descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
            } else {
                descriptor.setDescriptorVersion(DescriptorVersion.V2.getVersion());
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
     * validate the customIngress of the plugin descriptor.
     *
     * @param descriptor the plugin descriptor to validate
     * @throws InvalidBundleException if the one of the values is not the expected one
     */
    private PluginDescriptor validateCustomIngressPathOrThrow(PluginDescriptor descriptor) {

        if (!StringUtils.isEmpty(descriptor.getIngressPath())) {

            String ingressPath = BundleUtilities.composeIngressPathFromIngressPathProperty(descriptor);
            String[] splitIngressPath = ingressPath.split("/");

            if (splitIngressPath.length >= 1
                    && ValidationFunctions.VALID_ENTITY_CODE_REGEX_PATTERN.matcher(splitIngressPath[1]).matches()) {

                throw new InvalidBundleException(String.format(INVALID_CUSTOM_INGRESS_PATH, descriptor.getName(),
                        ingressPath));
            }
        }
        return descriptor;
    }

    private PluginDescriptor validateRolesLengthOrThrow(PluginDescriptor descriptor) {
        List<String> rolesList = descriptor.getRoles();
        if (rolesList != null && !rolesList.isEmpty()) {

            String roles = rolesList.stream().collect(Collectors.joining(","));
            if (roles.length() > ROLES_MAX_LENGTH) {
                throw new InvalidBundleException(
                        String.format(INVALID_ROLES_MAX_LENGTH_EXCEEDED_ERROR, roles, ROLES_MAX_LENGTH));
            }
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
        if (descriptor.getDescriptorMetadata().getPluginCode().length() > fullDeploymentNameMaxlength) {

            throw new EntandoComponentManagerException(
                    String.format(
                            DEPLOYMENT_BASE_NAME_MAX_LENGTH_EXCEEDED_ERROR,
                            descriptor.getDescriptorMetadata().getPluginCode(),
                            fullDeploymentNameMaxlength));
        }

        return descriptor;
    }


    /**
     * validate the PluginResources property. if the validation fails an EntandoComponentManagerException is thrown.
     *
     * @param pluginDescriptor the descriptor of which validate the fullDeploymentName length
     * @return the validated PluginDescriptor
     */
    private PluginDescriptor validatePluginResources(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getResources() == null) {
            return pluginDescriptor;
        }

        final PluginResources pluginResources = pluginDescriptor.getResources();

        // check storage measure unit
        validatePluginResourcePropOrThrow(MEM_AND_STORAGE_PATTERN, pluginDescriptor.getName(), "storage",
                pluginResources.getStorage());

        // check memory measure unit
        validatePluginResourcePropOrThrow(MEM_AND_STORAGE_PATTERN, pluginDescriptor.getName(), "memory",
                pluginResources.getMemory());

        // check cpu measure unit
        validatePluginResourcePropOrThrow(CPU_PATTERN, pluginDescriptor.getName(), "cpu",
                pluginResources.getCpu());

        return pluginDescriptor;
    }

    private static void validatePluginResourcePropOrThrow(Pattern regexPattern, String pluginName, String propName, String propValue) {
        if (! regexPattern.matcher(propValue).matches()) {
            throw new EntandoComponentManagerException(String.format(
                    PLUGIN_RESOURCES_NOT_VALID_ERROR,
                    propName, pluginName, propValue));
        }
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
    public static final String INVALID_CUSTOM_INGRESS_PATH =
            "The plugin \"%s\" contains an invalid custom ingress path: \"%s\". Custom ingress paths cannot mime the "
                    + "standard format in which the first subpath matches with the regex \""
                    + ValidationFunctions.VALID_ENTITY_CODE_REGEX + "\"";

    public static final String INVALID_ROLES_MAX_LENGTH_EXCEEDED_ERROR =
            "The roles (joined with comma) \"%s\" exceeds the max allowed length \"%d\".";

    public static final String PLUGIN_RESOURCES_NOT_VALID_ERROR =
            "The plugin resources \"%s\" of the plugin \"%s\""
                    + "contains an invalid value: %s. Accepted values for memory and storage must match the regex "
                    + MEM_AND_STORAGE_REGEX + " . Accepted values for CPU must match the regex " + CPU_REGEX;

}
