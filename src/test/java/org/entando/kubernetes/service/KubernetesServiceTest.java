package org.entando.kubernetes.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.awaitility.core.ConditionFactory;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.k8ssvc.EntandoAppPluginLinkingProcessException;
import org.entando.kubernetes.model.common.EntandoDeploymentPhase;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
public class KubernetesServiceTest {

    private static final String APP_NAME = "test-app";
    private static final String APP_NAMESPACE = "test-namespace";
    private static final Set<String> DIGITAL_EXCHANGES_NAMES = Set
            .of("namespace-1", TestEntitiesGenerator.DEFAULT_BUNDLE_NAMESPACE);
    private static final String PLUGIN_NAME = "my-plugin";
    private KubernetesService kubernetesService;
    private K8SServiceClient client;

    @BeforeEach
    public void setup() {
        client = new K8SServiceClientTestDouble();
        kubernetesService = new KubernetesService(APP_NAME, APP_NAMESPACE, DIGITAL_EXCHANGES_NAMES, client, composeCf());
    }

    private ConditionFactory composeCf() {
        return await().atMost(Duration.ofMinutes(1))
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofSeconds(10));
    }

    @AfterEach
    public void cleanup() {
        getClientDouble().cleanInMemoryDatabases();
    }

    @Test
    public void shouldReturnFalseIfPluginCustomResourceStatusIsFailed() {
        EntandoPlugin plugin = getTestEntandoPlugin();
        EntandoAppPluginLink link = getTestEntandoAppPluginLink();

        boolean val = kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin, true);
        assertFalse(val);
    }

    @Test
    public void shouldReturnTrueIfPluginIsDeployedAndLinkIsDeployedAndPluginIsAccessibleFromApp() {

        EntandoPlugin plugin = getTestEntandoPlugin();
        plugin.getStatus().updateDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL, 1L);

        EntandoAppPluginLink link = getTestEntandoAppPluginLink();
        link.getStatus().updateDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL, 1L);

        getClientDouble().addInMemoryLink(link);
        getClientDouble().addInMemoryLinkedPlugins(plugin);

        assertTrue(kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin, true));
    }

    @Test
    public void shouldThrowAnExceptionIfLinkDeploymentFailed() {
        Assertions.assertThrows(EntandoAppPluginLinkingProcessException.class, () -> {
            EntandoPlugin plugin = getTestEntandoPlugin();
            plugin.getStatus().updateDeploymentPhase(EntandoDeploymentPhase.SUCCESSFUL, 1L);

            EntandoAppPluginLink link = getTestEntandoAppPluginLink();
            link.getStatus().updateDeploymentPhase(EntandoDeploymentPhase.FAILED, 1L);

            getClientDouble().addInMemoryLink(link);
            getClientDouble().addInMemoryLinkedPlugins(plugin);

            kubernetesService.hasLinkingProcessCompletedSuccessfully(link, plugin, true);
        });
    }

    @Test
    void shouldGetBundleByNameIfEntandoDigitalExchangesNamesIsNullOrEmpty() {
        // given that I have a bundle
        final EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();

        // instruct the mock to return the bundle only if getBundleWithName method is called
        final K8SServiceClient mockK8SServiceClient = Mockito.mock(K8SServiceClient.class);
        when(mockK8SServiceClient.getBundleWithName(any())).thenReturn(Optional.of(bundle));
        when(mockK8SServiceClient
                .getBundleWithNameAndNamespace(bundle.getSpec().getDetails().getName(),
                        bundle.getMetadata().getNamespace()))
                .thenReturn(Optional.empty());

        Set<String> emptyDigExNames = Set.of();
        Stream.of(emptyDigExNames, null).forEach(digitalExchangeNames -> {
            KubernetesService kubernetesService = new KubernetesService(APP_NAME, APP_NAMESPACE, digitalExchangeNames,
                    mockK8SServiceClient, composeCf());

            final Optional<EntandoDeBundle> entandoDeBundleOpt = kubernetesService
                    .fetchBundleByName(bundle.getSpec().getDetails().getName());

            assertThat(entandoDeBundleOpt.isPresent()).isTrue();
        });
    }

    @Test
    void shouldGetBundleByNameAndNamespaceIfEntandoDigitalExchangesNamesHasValue() {
        // given that I have a bundle
        final EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();

        // instruct the mock to return the bundle only if getBundleWithNameAndNamespace method is called
        final K8SServiceClient mockK8SServiceClient = Mockito.mock(K8SServiceClient.class);
        when(mockK8SServiceClient.getBundleWithName(any())).thenReturn(Optional.empty());
        when(mockK8SServiceClient
                .getBundleWithNameAndNamespace(bundle.getSpec().getDetails().getName(),
                        bundle.getMetadata().getNamespace()))
                .thenReturn(Optional.of(bundle));

        KubernetesService kubernetesService = new KubernetesService(APP_NAME, APP_NAMESPACE, DIGITAL_EXCHANGES_NAMES, mockK8SServiceClient,
                composeCf());

        final Optional<EntandoDeBundle> entandoDeBundleOpt = kubernetesService
                .fetchBundleByName(bundle.getSpec().getDetails().getName());

        assertThat(entandoDeBundleOpt.isPresent()).isTrue();
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
