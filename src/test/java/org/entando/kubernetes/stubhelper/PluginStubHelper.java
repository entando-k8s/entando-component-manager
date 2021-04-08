package org.entando.kubernetes.stubhelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Spec;

public class PluginStubHelper {

    public static final String EXPECTED_PLUGIN_NAME = "entando-the-lucas";
    public static final String EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME = "customdepbasename";
    public static final String EXPECTED_INGRESS_PATH = "/entando/the-lucas";
    public static final String TEST_DESCRIPTOR_IMAGE = "entando/the-lucas:0.0.1-SNAPSHOT";
    public static final String TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME = "customDepBaseName";
    public static final String TEST_DESCRIPTOR_ADMIN_ROLE = "thelucas-admin";
    public static final String TEST_DESCRIPTOR_USER_ROLE = "thelucas-user";
    public static final String TEST_DESCRIPTOR_HEALTH_PATH = "/management/health";
    public static final String TEST_DESCRIPTOR_DBMS = "postgresql";
    public static final String TEST_DESCRIPTOR_SECURITY_LEVEL = "lenient";


    public static PluginDescriptor stubPluginDescriptorV2() {
        return PluginDescriptor.builder()
                .image(TEST_DESCRIPTOR_IMAGE)
                .roles(Arrays.asList(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE))
                .healthCheckPath(TEST_DESCRIPTOR_HEALTH_PATH)
                .dbms(TEST_DESCRIPTOR_DBMS)
                .deploymentBaseName(TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME)
                .build();
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
