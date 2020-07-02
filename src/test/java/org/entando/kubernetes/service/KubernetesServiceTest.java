package org.entando.kubernetes.service;

import org.awaitility.core.ConditionFactory;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.k8ssvc.EntandoAppPluginLinkingProcessException;
import org.entando.kubernetes.model.EntandoDeploymentPhase;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class KubernetesServiceTest {

    private KubernetesService kubernetesService;
    private K8SServiceClient client;
    private final String APP_NAME = "test-app";
    private final String APP_NAMESPACE = "test-namespace";
    private final String PLUGIN_NAME = "my-plugin";


    @BeforeEach
    public void setup() {
        ConditionFactory cf = await().atMost(Duration.ofMinutes(1))
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofSeconds(10));
        client = new K8SServiceClientTestDouble();
        kubernetesService = new KubernetesService(APP_NAME, APP_NAMESPACE, client, cf);
    }

    @AfterEach
    public void cleanup() {
        getClientDouble().cleanInMemoryDatabases();
    }

    @Test
    public void shouldReturnFalseIfPluginCustomResourceStatusIsFailed() {
        EntandoPlugin plugin = getTestEntandoPlugin();
        EntandoAppPluginLink link = getTestEntandoAppPluginLink();

        boolean val = kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin);
        assertFalse(val);
    }

    @Test
    public void shouldReturnTrueIfPluginIsDeployedAndLinkIsDeployedAndPluginIsAccessibleFromApp() {

        EntandoPlugin plugin = getTestEntandoPlugin();
        plugin.getStatus().setEntandoDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL);

        EntandoAppPluginLink link = getTestEntandoAppPluginLink();
        link.getStatus().setEntandoDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL);

        getClientDouble().addInMemoryLink(link);
        getClientDouble().addInMemoryLinkedPlugins(plugin);

        assertTrue(kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin));
    }

    @Test
    public void shouldThrowAnExceptionIfLinkDeploymentFailed() {
        Assertions.assertThrows(EntandoAppPluginLinkingProcessException.class, () -> {
            EntandoPlugin plugin = getTestEntandoPlugin();
            plugin.getStatus().setEntandoDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL);

            EntandoAppPluginLink link = getTestEntandoAppPluginLink();
            link.getStatus().setEntandoDeploymentPhase(EntandoDeploymentPhase.FAILED);

            getClientDouble().addInMemoryLink(link);
            getClientDouble().addInMemoryLinkedPlugins(plugin);

            kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin);
        });
    }

    private EntandoAppPluginLink getTestEntandoAppPluginLink() {
        return new EntandoAppPluginLinkBuilder()
                .withNewMetadata()
                .withName(String.format("%s-to-%s-link", APP_NAME, PLUGIN_NAME))
                .withNamespace(APP_NAMESPACE)
                .endMetadata()
                .withNewSpec()
                .withEntandoPlugin(APP_NAMESPACE, PLUGIN_NAME)
                .withEntandoApp(APP_NAMESPACE, APP_NAME)
                .endSpec()
                .build();
    }

    private EntandoPlugin getTestEntandoPlugin() {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(PLUGIN_NAME)
                .withNamespace(APP_NAMESPACE)
                .endMetadata()
                .withNewSpec()
                .withIngressPath("/my-plugin")
                .endSpec()
                .build();
    }


    public K8SServiceClientTestDouble getClientDouble() {
        return (K8SServiceClientTestDouble) client;
    }
}
