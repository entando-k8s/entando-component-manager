package org.entando.kubernetes.controller.mockmvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.exception.k8ssvc.PluginNotFoundException;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.service.KubernetesService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class})
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class PluginControllerTest {

    private static final String URL = "/plugins";
    private static final String URL_INFO = "/plugins/info";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KubernetesService kubernetesService;


    @Test
    public void testListEmpty() throws Exception {
        when(kubernetesService.getLinkedPlugins()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));
    }

    @Test
    public void testNotFound() throws Exception {
        String pluginId = "arbitrary-plugin";
        when(kubernetesService.getLinkedPlugin(anyString())).thenThrow(new PluginNotFoundException());

        mockMvc.perform(get(String.format("%s/%s", URL, pluginId)))
                .andDo(print()).andExpect(status().isNotFound());

    }

    @Test
    public void testList() throws Exception {
        List<EntandoPlugin> linkedPlugins = Collections.singletonList(getTestEntandoPlugin());
        when(kubernetesService.getLinkedPlugins()).thenReturn(linkedPlugins);

        mockMvc.perform(get(URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)));

    }

    @Test
    public void testSinglePlugin() throws Exception {
        when(kubernetesService.getLinkedPlugin(anyString())).thenReturn(getTestEntandoPlugin());

        mockMvc.perform(get(URL))
                .andDo(print()).andExpect(status().isOk());

    }

    @Test
    public void testListPluginInfoEmpty() throws Exception {
        when(kubernetesService.getLinkedPlugins()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(URL_INFO))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));
    }

    @Test
    public void testListPluginInfoAllData() throws Exception {
        List<EntandoPlugin> linkedPlugins = Collections.singletonList(getTestEntandoPluginInfoAllData());
        when(kubernetesService.getLinkedPlugins()).thenReturn(linkedPlugins);

        mockMvc.perform(get(URL_INFO))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0].id", is("plugin-info-uid")))
                .andExpect(jsonPath("payload[0].name", is("plugin-info-name")))
                .andExpect(jsonPath("payload[0].description", is("plugin-info-description")));
    }

    @Test
    public void testListPluginInfoOnlyId() throws Exception {
        List<EntandoPlugin> linkedPlugins = Collections.singletonList(getTestEntandoPluginInfoOnlyId());
        when(kubernetesService.getLinkedPlugins()).thenReturn(linkedPlugins);

        mockMvc.perform(get(URL_INFO))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))

                .andExpect(jsonPath("payload[0].id", is("plugin-info-uid")))
                .andExpect(jsonPath("payload[0].name", nullValue()))
                .andExpect(jsonPath("payload[0].description", nullValue()));
    }

    @Test
    public void testListPluginInfoOnlyName() throws Exception {
        List<EntandoPlugin> linkedPlugins = Collections.singletonList(getTestEntandoPluginInfoOnlyName());
        when(kubernetesService.getLinkedPlugins()).thenReturn(linkedPlugins);

        mockMvc.perform(get(URL_INFO))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0].id", nullValue()))
                .andExpect(jsonPath("payload[0].name", is("plugin-info-name")))
                .andExpect(jsonPath("payload[0].description", nullValue()));
    }

    @Test
    public void testListPluginInfoOnlyDescription() throws Exception {
        List<EntandoPlugin> linkedPlugins = Collections.singletonList(getTestEntandoPluginInfoOnlyDescription());
        when(kubernetesService.getLinkedPlugins()).thenReturn(linkedPlugins);

        mockMvc.perform(get(URL_INFO))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0].id", nullValue()))
                .andExpect(jsonPath("payload[0].name", nullValue()))
                .andExpect(jsonPath("payload[0].description", is("plugin-info-description")));
    }

    private EntandoPlugin getTestEntandoPlugin() {
        return new EntandoPluginBuilder()
                .withNewSpec()
                .withReplicas(1)
                .withIngressPath("/pluginpath")
                .endSpec()
                .withNewMetadata()
                .withName("plugin-name")
                .endMetadata()
                .build();
    }

    private EntandoPlugin getTestEntandoPluginInfoAllData() {
        return new EntandoPluginBuilder()
                .withNewSpec()
                .withReplicas(1)
                .withIngressPath("/pluginpath")
                .endSpec()
                .withNewMetadata()
                .withUid("plugin-info-uid")
                .withName("plugin-info-name")
                .withAnnotations(Collections.singletonMap("description", "plugin-info-description"))
                .endMetadata()
                .build();
    }

    private EntandoPlugin getTestEntandoPluginInfoOnlyId() {
        return new EntandoPluginBuilder()
                .withNewSpec()
                .withReplicas(1)
                .withIngressPath("/pluginpath")
                .endSpec()
                .withNewMetadata()
                .withUid("plugin-info-uid")
                .endMetadata()
                .build();
    }

    private EntandoPlugin getTestEntandoPluginInfoOnlyName() {
        return new EntandoPluginBuilder()
                .withNewSpec()
                .withReplicas(1)
                .withIngressPath("/pluginpath")
                .endSpec()
                .withNewMetadata()
                .withName("plugin-info-name")
                .endMetadata()
                .build();
    }

    private EntandoPlugin getTestEntandoPluginInfoOnlyDescription() {
        return new EntandoPluginBuilder()
                .withNewSpec()
                .withReplicas(1)
                .withIngressPath("/pluginpath")
                .endSpec()
                .withNewMetadata()
                .withAnnotations(Collections.singletonMap("description", "plugin-info-description"))
                .endMetadata()
                .build();
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
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].lastTransitionTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "serverStatus.podStatus.conditions[0].type").value("Initialized"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.availableReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.readyReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.replicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.updatedReplicas").value(1))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].lastTransitionTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].lastUpdateTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].type").value("Progressing"))
                .andExpect(jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].reason")
                        .value("NewReplicaSetAvailable"))
                .andExpect(
                        jsonPath(prefix + "serverStatus.deploymentStatus.conditions[0].message").value("Some message"))

                .andExpect(jsonPath(prefix + "externalServiceStatuses", hasSize(1)))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].type").value("dbServer"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].replicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].volumePhase").value("Bound"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.phase").value("Running"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions", hasSize(2)))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].lastTransitionTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].status").value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[0].type")
                        .value("Initialized"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].lastTransitionTime")
                        .value("2019-07-11T18:36:09Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].status").value("True"))
                .andExpect(
                        jsonPath(prefix + "externalServiceStatuses[0].podStatus.conditions[1].type").value("Available"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.availableReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.readyReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.replicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.updatedReplicas").value(1))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions", hasSize(2)))
                .andExpect(jsonPath(
                        prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].lastTransitionTime")
                        .value("2019-07-11T18:36:03Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].lastUpdateTime")
                        .value("2019-07-11T18:36:03Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].status")
                        .value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].type")
                        .value("Progressing"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].reason")
                        .value("NewReplicaSetAvailable"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[0].message")
                        .value("Some message"))
                .andExpect(jsonPath(
                        prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].lastTransitionTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].lastUpdateTime")
                        .value("2019-07-11T18:36:06Z"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].status")
                        .value("True"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].type")
                        .value("Available"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].reason")
                        .value("MinimumReplicasAvailable"))
                .andExpect(jsonPath(prefix + "externalServiceStatuses[0].deploymentStatus.conditions[1].message")
                        .value("Some message"))
        ;
    }

}
