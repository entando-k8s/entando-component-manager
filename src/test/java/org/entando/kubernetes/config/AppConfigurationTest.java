package org.entando.kubernetes.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.processor.CategoryProcessor;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTypeProcessor;
import org.entando.kubernetes.model.bundle.processor.DirectoryProcessor;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.bundle.processor.FragmentProcessor;
import org.entando.kubernetes.model.bundle.processor.GroupProcessor;
import org.entando.kubernetes.model.bundle.processor.LabelProcessor;
import org.entando.kubernetes.model.bundle.processor.LanguageProcessor;
import org.entando.kubernetes.model.bundle.processor.PageProcessor;
import org.entando.kubernetes.model.bundle.processor.PageTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.service.KubernetesService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class AppConfigurationTest {

    @Test
    void testPageProcessor() {
        AppConfiguration app = new AppConfiguration();

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansOfType(ComponentProcessor.class)).thenReturn(allProcessors());

        ComponentType[] expected = Arrays.stream(ComponentType.values())
                .toArray(ComponentType[]::new);

        assertThat(app.processorMap(context).keySet()).containsExactlyInAnyOrder(expected);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, ComponentProcessor> allProcessors() {
        EntandoCoreClient coreClient = mock(EntandoCoreClient.class);
        KubernetesService k8sService = mock(KubernetesService.class);

        Map<String, ComponentProcessor> processors = new HashMap<>();
        processors.put(ComponentType.CONTENT_TEMPLATE.toString(), new ContentTemplateProcessor(coreClient));
        processors.put(ComponentType.CONTENT_TYPE.toString(), new ContentTypeProcessor(coreClient));
        processors.put(ComponentType.DIRECTORY.toString(), new DirectoryProcessor(coreClient));
        processors.put(ComponentType.ASSET.toString(), new FileProcessor(coreClient));
        processors.put(ComponentType.FRAGMENT.toString(), new FragmentProcessor(coreClient));
        processors.put(ComponentType.LANGUAGE.toString(), new LanguageProcessor(coreClient));
        processors.put(ComponentType.LABEL.toString(), new LabelProcessor(coreClient));
        processors.put(ComponentType.PAGE.toString(), new PageProcessor(coreClient));
        processors.put(ComponentType.PAGE_TEMPLATE.toString(), new PageTemplateProcessor(coreClient));
        processors.put(ComponentType.PLUGIN.toString(), new PluginProcessor(k8sService));
        processors.put(ComponentType.WIDGET.toString(), new WidgetProcessor(coreClient));
        processors.put(ComponentType.GROUP.toString(), new GroupProcessor(coreClient));
        processors.put(ComponentType.CATEGORY.toString(), new CategoryProcessor(coreClient));

        return processors;
    }

}
