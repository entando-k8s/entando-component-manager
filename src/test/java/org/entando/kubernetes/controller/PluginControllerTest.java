package org.entando.kubernetes.controller;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.kubernetes.service.KubernetesService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    public void testList() throws Exception {
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

}
