package org.entando.kubernetes.stubhelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Spec;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginResources;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.bundle.descriptor.plugin.ValueFrom;

public class PluginStubHelper {

    public static final String BUNDLE_NAME = "my-bundle";
    public static final String BUNDLE_ID = "a1b2c3d4";
    public static final String BUNDLE_CODE = BUNDLE_NAME + "-" + BUNDLE_ID;
    public static final String EXPECTED_PLUGIN_NAME = "entando-the-lucas";
    public static final String EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME = "customdepbasename";
    public static final String EXPECTED_INGRESS_PATH_V_MINOR_THAN_3 = "/entando/the-lucas/0-0-1-snapshot";
    public static final String EXPECTED_INGRESS_PATH_V_3_OR_V_4 = "/entando/the-lucas";
    public static final String EXPECTED_INGRESS_PATH_V_5 = "/" + BUNDLE_CODE + "-986a1b71/" + BUNDLE_NAME;
    public static final String TEST_DESCRIPTOR_IMAGE = "entando/the-lucas:0.0.1-SNAPSHOT";
    public static final String TEST_DESCRIPTOR_IMAGE_SHA = "24f085aa";
    public static final String TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME = "customDepBaseName";
    public static final String TEST_DESCRIPTOR_ADMIN_ROLE = "thelucas-admin";
    public static final String TEST_DESCRIPTOR_USER_ROLE = "thelucas-user";
    public static final String TEST_DESCRIPTOR_HEALTH_PATH = "/management/health";
    public static final String TEST_DESCRIPTOR_DBMS = "postgresql";
    public static final String TEST_DESCRIPTOR_SECURITY_LEVEL = "lenient";
    public static final String TEST_ENV_VAR_0_NAME = "ENTANDO_TENANT_CODE";
    public static final String TEST_ENV_VAR_0_VALUE = "primary";
    public static final String TEST_ENV_VAR_1_NAME = "env1Name";
    public static final String TEST_ENV_VAR_1_VALUE = "env1Value";
    public static final String TEST_ENV_VAR_2_NAME = "env2Name";
    public static final String TEST_ENV_VAR_2_SECRET_NAME = BUNDLE_ID + "-env-2-secret-name";
    public static final String TEST_ENV_VAR_2_SECRET_KEY = "env2SecretKey";
    public static final String TEST_ENV_VAR_3_NAME = "env3Name";
    public static final String PLUGIN_IMAGE_SHA = "sha256:f1acf3443c577db1b1df3b47593b07895acab5ef582e5a661c2c1ade6f19e431";
    public static final String TEST_DESCRIPTOR_IMAGE_WITH_SHA = "entando/the-lucas@" + PLUGIN_IMAGE_SHA;
    public static final String TEST_RES_STORAGE = "7G";
    public static final String TEST_RES_MEMORY = "8G";
    public static final String TEST_RES_CPU = "6m";
    public static final String PRIMARY_TENANT_CODE = "primary";
    public static final String SECONDARY_TENANT_CODE = "secondary";


    public static PluginDescriptor stubPluginDescriptorV2() {
        return PluginDescriptor.builder()
                .image(TEST_DESCRIPTOR_IMAGE)
                .roles(Arrays.asList(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE))
                .healthCheckPath(TEST_DESCRIPTOR_HEALTH_PATH)
                .dbms(TEST_DESCRIPTOR_DBMS)
                .deploymentBaseName(TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME)
                .build()
                .setDescriptorMetadata(stubDescriptorMetadata());
    }

    public static PluginDescriptor stubPluginDescriptorV3() {
        PluginDescriptor pluginDescriptorV3 = stubPluginDescriptorV2();
        pluginDescriptorV3.setDescriptorVersion(DescriptorVersion.V3.getVersion());
        return pluginDescriptorV3;
    }

    public static PluginDescriptor stubPluginDescriptorV4() {
        PluginDescriptor pluginDescriptorV3 = stubPluginDescriptorV2();
        pluginDescriptorV3.setDescriptorVersion(DescriptorVersion.V4.getVersion());
        return pluginDescriptorV3;
    }

    public static PluginDescriptor stubPluginDescriptorV4WithEnvVars() {
        PluginDescriptor pluginDescriptor = stubPluginDescriptorV4();
        pluginDescriptor.setEnvironmentVariables(stubEnvironmentVariables());
        return pluginDescriptor;
    }

    public static PluginDescriptor stubPluginDescriptorV5() {
        PluginDescriptor pluginDescriptorV5 = stubPluginDescriptorV2()
                .setName(BUNDLE_NAME);
        pluginDescriptorV5.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        pluginDescriptorV5.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);
        return pluginDescriptorV5;
    }

    public static PluginDescriptor stubPluginDescriptorV6() {
        PluginDescriptor pluginDescriptor = stubPluginDescriptorV5()
                .setResources(stubPluginResources());
        pluginDescriptor.setDescriptorVersion(DescriptorVersion.V6.getVersion());
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

    public static PluginDescriptor.DescriptorMetadata stubDescriptorMetadata() {
        return new DescriptorMetadata(BUNDLE_ID,
                BUNDLE_CODE,
                TEST_DESCRIPTOR_IMAGE_SHA,
                EXPECTED_PLUGIN_NAME,
                TEST_DESCRIPTOR_IMAGE_SHA + "-" + EXPECTED_PLUGIN_NAME,
                TEST_DESCRIPTOR_IMAGE_SHA + "-" + EXPECTED_PLUGIN_NAME,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4,
                PluginStubHelper.PRIMARY_TENANT_CODE);
    }

    public static PluginResources stubPluginResources() {
        return new PluginResources()
                .setStorage(TEST_RES_STORAGE)
                .setMemory(TEST_RES_MEMORY)
                .setCpu(TEST_RES_CPU);
    }
}
