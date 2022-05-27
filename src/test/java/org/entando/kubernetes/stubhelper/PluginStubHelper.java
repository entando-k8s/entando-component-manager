package org.entando.kubernetes.stubhelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Spec;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.bundle.descriptor.plugin.ValueFrom;

public class PluginStubHelper {

    public static final String BUNDLE_ID = "my-bundle";
    public static final String EXPECTED_PLUGIN_NAME = "entando-the-lucas";
    public static final String EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME = "customdepbasename";
    public static final String EXPECTED_INGRESS_PATH_V_MINOR_THAN_3 = "/entando/the-lucas/0-0-1-snapshot";
    public static final String EXPECTED_INGRESS_PATH_V_EQUAL_OR_MAJOR_THAN_3 = "/entando/the-lucas";
    public static final String TEST_DESCRIPTOR_IMAGE = "entando/the-lucas:0.0.1-SNAPSHOT";
    public static final String TEST_DESCRIPTOR_IMAGE_SHA = "24f085aa";
    public static final String TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME = "customDepBaseName";
    public static final String TEST_DESCRIPTOR_ADMIN_ROLE = "thelucas-admin";
    public static final String TEST_DESCRIPTOR_USER_ROLE = "thelucas-user";
    public static final String TEST_DESCRIPTOR_HEALTH_PATH = "/management/health";
    public static final String TEST_DESCRIPTOR_DBMS = "postgresql";
    public static final String TEST_DESCRIPTOR_SECURITY_LEVEL = "lenient";
    public static final String TEST_ENV_VAR_1_NAME = "env1Name";
    public static final String TEST_ENV_VAR_1_VALUE = "env1Value";
    public static final String TEST_ENV_VAR_2_NAME = "env2Name";
    public static final String TEST_ENV_VAR_2_SECRET_NAME =
            BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "-env-2-secret-name";
    public static final String TEST_ENV_VAR_2_SECRET_KEY = "env2SecretKey";


    public static PluginDescriptor stubPluginDescriptorV2() {
        return PluginDescriptor.builder()
                .image(TEST_DESCRIPTOR_IMAGE)
                .roles(Arrays.asList(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE))
                .healthCheckPath(TEST_DESCRIPTOR_HEALTH_PATH)
                .dbms(TEST_DESCRIPTOR_DBMS)
                .deploymentBaseName(TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME)
                .build()
                .setDescriptorMetadata(BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA, EXPECTED_PLUGIN_NAME,
                        TEST_DESCRIPTOR_IMAGE_SHA);
    }

    public static PluginDescriptor stubPluginDescriptorV3() {
        PluginDescriptor pluginDescriptorV3 = stubPluginDescriptorV2();
        pluginDescriptorV3.setDescriptorVersion(PluginDescriptorVersion.V3.getVersion());
        return pluginDescriptorV3;
    }

    public static PluginDescriptor stubPluginDescriptorV4() {
        PluginDescriptor pluginDescriptorV3 = stubPluginDescriptorV2();
        pluginDescriptorV3.setDescriptorVersion(PluginDescriptorVersion.V4.getVersion());
        return pluginDescriptorV3;
    }

    public static PluginDescriptor stubPluginDescriptorV4WithEnvVars() {
        PluginDescriptor pluginDescriptor = stubPluginDescriptorV4();
        pluginDescriptor.setEnvironmentVariables(stubEnvironmentVariables());
        return pluginDescriptor;
    }

    public static List<EnvironmentVariable> stubEnvironmentVariables() {
        return Arrays.asList(
                new EnvironmentVariable(TEST_ENV_VAR_1_NAME, TEST_ENV_VAR_1_VALUE, null),
                new EnvironmentVariable(TEST_ENV_VAR_2_NAME, null,
                        new ValueFrom(
                                new SecretKeyRef(TEST_ENV_VAR_2_SECRET_NAME, TEST_ENV_VAR_2_SECRET_KEY))
                )
        );
    }

    public static PluginDescriptor stubPluginDescriptorV1() {
        return PluginDescriptor.builder()
                .spec(stubPluginDescriptorV1Spec())
                .build();
    }

    public static PluginDescriptorV1Spec stubPluginDescriptorV1Spec() {

        List<PluginDescriptorV1Role> roleList = Stream.of(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE)
                .map(role -> new PluginDescriptorV1Role(role, role))
                .collect(Collectors.toList());

        return new PluginDescriptorV1Spec()
                .setImage(TEST_DESCRIPTOR_IMAGE)
                .setRoles(roleList)
                .setHealthCheckPath(TEST_DESCRIPTOR_HEALTH_PATH)
                .setDbms(TEST_DESCRIPTOR_DBMS)
                .setSecurityLevel(TEST_DESCRIPTOR_SECURITY_LEVEL);
    }
}
