package org.entando.kubernetes.controller.mockmvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.service.KubernetesService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class})
@ActiveProfiles({"test"})
@Tag("component")
public class PluginInstallTest {

    private static final String DIGITAL_EXCHANGE_ID = "community";
    private static final String DIGITAL_EXCHANGE_URL = "https://community.de.entando.org";

    @Autowired
    private KubernetesService kubernetesService;

    @Autowired
    private K8SServiceClient k8SServiceClient;


    @Test
    public void testDeployment() {
        EntandoPlugin entandoPlugin = new EntandoPluginBuilder()
                .withNewMetadata()
                    .withName("avatar-plugin")
                .endMetadata()
                .withNewSpec()
                    .withImage("entando/entando-avatar-plugin")
                    .withIngressPath("/avatar")
                    .withHealthCheckPath("/actuator/health")
                    .withDbms(DbmsVendor.MYSQL)
                    .addNewRole("read", "Read")
                    .addNewPermission("another-client", "read")
                .endSpec()
                .build();

        final DigitalExchange digitalExchange = new DigitalExchange();
        digitalExchange.setId(DIGITAL_EXCHANGE_ID);
        digitalExchange.setUrl(DIGITAL_EXCHANGE_URL);

        kubernetesService.linkPlugin(entandoPlugin);

        K8SServiceClientTestDouble k8sSvcClient = (K8SServiceClientTestDouble)  k8SServiceClient;
        List<EntandoPlugin> linkedPluginDatabase = k8sSvcClient.getInMemoryPluginsCopy();
        assertThat(linkedPluginDatabase.size()).isEqualTo(1);
        EntandoPlugin plugin = linkedPluginDatabase.get(0);

        assertThat(plugin.getSpec().getIngressPath()).isEqualTo("/avatar");
        assertThat(plugin.getSpec().getDbms()).isEqualTo(Optional.of(DbmsVendor.MYSQL));
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/entando-avatar-plugin");
        assertThat(plugin.getSpec().getHealthCheckPath()).isEqualTo("/actuator/health");
        assertThat(plugin.getSpec().getReplicas()).isEqualTo(Optional.of(1));
        assertThat(plugin.getMetadata().getName()).isEqualTo("avatar-plugin");

        assertThat(plugin.getSpec().getRoles()).hasSize(1);
        assertThat(plugin.getSpec().getRoles().get(0).getCode()).isEqualTo("read");
        assertThat(plugin.getSpec().getRoles().get(0).getName()).isEqualTo("Read");

        assertThat(plugin.getSpec().getPermissions()).hasSize(1);
        assertThat(plugin.getSpec().getPermissions().get(0).getClientId()).isEqualTo("another-client");
        assertThat(plugin.getSpec().getPermissions().get(0).getRole()).isEqualTo("read");
    }

}