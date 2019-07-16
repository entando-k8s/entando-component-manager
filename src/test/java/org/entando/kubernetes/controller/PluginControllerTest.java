package org.entando.kubernetes.controller;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimStatus;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.entando.kubernetes.model.plugin.DbServerStatus;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoCustomResourceStatus;
import org.entando.kubernetes.model.plugin.EntandoDeploymentPhase;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.plugin.JeeServerStatus;
import org.entando.kubernetes.service.KubernetesService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class PluginControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private KubernetesClient client;

    @Mock private NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, DoneableCustomResourceDefinition,
            Resource<CustomResourceDefinition, DoneableCustomResourceDefinition>> resourceOperation;
    @Mock private NonNamespaceOperation<EntandoPlugin, EntandoPluginList, DoneableEntandoPlugin,
            Resource<EntandoPlugin, DoneableEntandoPlugin>> operation;
    @Mock private CustomResourceDefinition customResourceDefinition;
    @Mock private Resource<CustomResourceDefinition, DoneableCustomResourceDefinition> resource;
    @Mock private MixedOperation<EntandoPlugin, EntandoPluginList, DoneableEntandoPlugin,
            Resource<EntandoPlugin, DoneableEntandoPlugin>> mixedOperation;
    @Mock private EntandoPluginList pluginList;
    @Mock private Resource<EntandoPlugin, DoneableEntandoPlugin> pluginResource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        reset(client);
    }

    private void defineMocks() {
        when(client.customResourceDefinitions()).thenReturn(resourceOperation);
        when(resourceOperation.withName(KubernetesService.ENTANDOPLUGIN_CRD_NAME)).thenReturn(resource);
        when(resource.get()).thenReturn(customResourceDefinition);
        when(client.customResources(same(customResourceDefinition), same(EntandoPlugin.class),
                same(EntandoPluginList.class), same(DoneableEntandoPlugin.class)))
                .thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(anyString())).thenReturn(operation);
        when(operation.list()).thenReturn(pluginList);
        when(operation.withName(anyString())).thenReturn(pluginResource);
    }

    @Test
    public void testListEmpty() throws Exception {
        defineMocks();
        when(pluginList.getItems()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/plugin"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));
    }

    @Test
    public void testNotFound() throws Exception {
        final String pluginId = "arbitrary-plugin";

        defineMocks();
        when(pluginResource.get()).thenReturn(null);

        mockMvc.perform(get(String.format("/plugin/%s", pluginId)))
                .andDo(print()).andExpect(status().isNotFound());

        verify(operation, times(1)).withName(eq(pluginId));
    }

    @Test
    public void testList() throws Exception {
        defineMocks();

        final EntandoPlugin plugin = Mockito.mock(EntandoPlugin.class);
        final ObjectMeta metadata = Mockito.mock(ObjectMeta.class);
        final EntandoPluginSpec spec = Mockito.mock(EntandoPluginSpec.class);
        final EntandoCustomResourceStatus entandoStatus = Mockito.mock(EntandoCustomResourceStatus.class);
        final JeeServerStatus jeeServerStatus = Mockito.mock(JeeServerStatus.class);
        final DbServerStatus dbServerStatus = Mockito.mock(DbServerStatus.class);
        final DeploymentStatus jeeDeploymentStatus = Mockito.mock(DeploymentStatus.class);
        final DeploymentStatus dbDeploymentStatus = Mockito.mock(DeploymentStatus.class);
        final PersistentVolumeClaimStatus dbPvcStatus = Mockito.mock(PersistentVolumeClaimStatus.class);
        final PersistentVolumeClaimStatus jeePvcStatus = Mockito.mock(PersistentVolumeClaimStatus.class);
        final PodStatus jeePodStatus = Mockito.mock(PodStatus.class);
        final PodStatus dbPodStatus = Mockito.mock(PodStatus.class);

        when(plugin.getSpec()).thenReturn(spec);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(spec.getEntandoStatus()).thenReturn(entandoStatus);
        when(entandoStatus.getEntandoDeploymentPhase()).thenReturn(EntandoDeploymentPhase.SUCCESSFUL);
        when(entandoStatus.getDbServerStatus()).thenReturn(singletonList(dbServerStatus));
        when(entandoStatus.getJeeServerStatus()).thenReturn(singletonList(jeeServerStatus));
        when(dbServerStatus.getDeploymentStatus()).thenReturn(dbDeploymentStatus);
        when(dbServerStatus.getPodStatus()).thenReturn(dbPodStatus);
        when(dbServerStatus.getPersistentVolumeClaimStatus()).thenReturn(dbPvcStatus);
        when(jeeServerStatus.getDeploymentStatus()).thenReturn(jeeDeploymentStatus);
        when(jeeServerStatus.getPersistentVolumeClaimStatus()).thenReturn(jeePvcStatus);
        when(jeeServerStatus.getPodStatus()).thenReturn(jeePodStatus);

        // ---

        when(spec.getIngressPath()).thenReturn("/pluginpath");
        when(spec.getReplicas()).thenReturn(1);

        when(dbPodStatus.getPhase()).thenReturn("Running");
        when(dbPodStatus.getConditions()).thenReturn(asList(
            mockPodCondition("2019-07-11T18:36:09Z", "Available"),
            mockPodCondition("2019-07-11T18:36:06Z", "Initialized")
        ));
        when(dbDeploymentStatus.getReplicas()).thenReturn(1);
        when(dbDeploymentStatus.getAvailableReplicas()).thenReturn(1);
        when(dbDeploymentStatus.getReadyReplicas()).thenReturn(1);
        when(dbDeploymentStatus.getUpdatedReplicas()).thenReturn(1);
        when(dbDeploymentStatus.getConditions()).thenReturn(asList(
                mockDeploymentCondition("2019-07-11T18:36:06Z", "Some message",
                        "MinimumReplicasAvailable", "Available"),
                mockDeploymentCondition("2019-07-11T18:36:03Z", "Some message",
                        "NewReplicaSetAvailable", "Progressing")
        ));
        when(dbPvcStatus.getPhase()).thenReturn("Bound");

        // ---

        when(jeePodStatus.getPhase()).thenReturn("Running");
        when(jeePodStatus.getConditions()).thenReturn(singletonList(
                mockPodCondition("2019-07-11T18:36:06Z", "Initialized")));
        when(jeeDeploymentStatus.getReplicas()).thenReturn(1);
        when(jeeDeploymentStatus.getAvailableReplicas()).thenReturn(1);
        when(jeeDeploymentStatus.getReadyReplicas()).thenReturn(1);
        when(jeeDeploymentStatus.getUpdatedReplicas()).thenReturn(1);
        when(jeeDeploymentStatus.getConditions()).thenReturn(singletonList(
                mockDeploymentCondition("2019-07-11T18:36:06Z", "Some message",
                        "NewReplicaSetAvailable", "Progressing")));
        when(jeePvcStatus.getPhase()).thenReturn("Bound");

        when(metadata.getName()).thenReturn("plugin-name");
        when(pluginList.getItems()).thenReturn(singletonList(plugin));

        ResultActions resultActions = mockMvc.perform(get("/plugin"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)));
        validate(resultActions, "payload[0].");

        when(pluginResource.get()).thenReturn(plugin);
        resultActions = mockMvc.perform(get("/plugin/plugin-name"))
                .andDo(print()).andExpect(status().isOk());
        validate(resultActions, "payload.");
    }

    private void validate(final ResultActions actions, final String prefix) throws Exception {
        actions.andExpect(jsonPath(prefix + "plugin").value("plugin-name"))
                .andExpect(jsonPath(prefix + "online").value(true))
                .andExpect(jsonPath(prefix + "path").value("/pluginpath"))
                .andExpect(jsonPath(prefix + "replicas").value(1))
                .andExpect(jsonPath(prefix + "deploymentPhase").value("successful"))
                .andExpect(jsonPath(prefix + "serverStatus.type").value("jeeServer"))
                .andExpect(jsonPath(prefix + "serverStatus.replicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.volumePhase").value("Bound"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.phase").value("Running"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions", hasSize(1)))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].lastTransitionTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].type").value("Initialized"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.availableReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.readyReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.replicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.updatedReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].lastTransitionTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].lastUpdateTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].type").value("Progressing"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].reason").value("NewReplicaSetAvailable"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].message").value("Some message"))

                .andExpect(jsonPath(prefix + "externalServiceStatuses", hasSize(1)))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].type").value("dbServer"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].replicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].volumePhase").value("Bound"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.phase").value("Running"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions", hasSize(2)))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].lastTransitionTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].type").value("Initialized"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].lastTransitionTime").value("2019-07-11T18:36:09Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].status").value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].type").value("Available"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.availableReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.readyReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.replicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.updatedReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions", hasSize(2)))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].lastTransitionTime").value("2019-07-11T18:36:03Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].lastUpdateTime").value("2019-07-11T18:36:03Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].type").value("Progressing"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].reason").value("NewReplicaSetAvailable"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].message").value("Some message"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].lastTransitionTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].lastUpdateTime").value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].status").value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].type").value("Available"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].reason").value("MinimumReplicasAvailable"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].message").value("Some message"))
        ;
    }

    @Test
    public void testDeployment() throws Exception {
        defineMocks();
        final String json =
                "{" +
                "  \"image\": \"entando/entando-avatar-plugin\", \n" +
                "  \"plugin\": \"avatar-plugin\", \n" +
                "  \"ingressPath\": \"/avatar\", \n" +
                "  \"healthCheckPath\": \"/actuator/health\", \n" +
                "  \"dbms\": \"mysql\", \n" +
                "  \"roles\": [{ \"code\": \"read\", \"name\": \"Read\" }], \n" +
                "  \"permissions\": [{ \"clientId\": \"another-client\", \"role\": \"read\" }] \n" +
                "}";

        mockMvc.perform(post("/plugin/deploy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print()).andExpect(status().isOk());

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        verify(operation, times(1)).create(captor.capture());
        final EntandoPlugin plugin = captor.getValue();

        assertThat(plugin.getSpec().getIngressPath()).isEqualTo("/avatar");
        assertThat(plugin.getSpec().getDbms()).isEqualTo("mysql");
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/entando-avatar-plugin");
        assertThat(plugin.getSpec().getHealthCheckPath()).isEqualTo("/actuator/health");
        assertThat(plugin.getSpec().getReplicas()).isEqualTo(1);
        assertThat(plugin.getMetadata().getName()).isEqualTo("avatar-plugin");

        assertThat(plugin.getSpec().getRoles()).hasSize(1);
        assertThat(plugin.getSpec().getRoles().get(0).getCode()).isEqualTo("read");
        assertThat(plugin.getSpec().getRoles().get(0).getName()).isEqualTo("Read");

        assertThat(plugin.getSpec().getPermissions()).hasSize(1);
        assertThat(plugin.getSpec().getPermissions().get(0).getClientId()).isEqualTo("another-client");
        assertThat(plugin.getSpec().getPermissions().get(0).getRole()).isEqualTo("read");
    }

    private DeploymentCondition mockDeploymentCondition(final String ts, final String message,
                                              final String reason, final String type) {
        final DeploymentCondition condition = new DeploymentCondition();
        condition.setLastTransitionTime(ts);
        condition.setLastUpdateTime(ts);
        condition.setMessage(message);
        condition.setReason(reason);
        condition.setStatus("True");
        condition.setType(type);
        return condition;
    }

    private PodCondition mockPodCondition(final String ts, final String type) {
        final PodCondition condition = new PodCondition();
        condition.setLastTransitionTime(ts);
        condition.setStatus("True");
        condition.setType(type);
        return condition;
    }

}
