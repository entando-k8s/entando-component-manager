package org.entando.kubernetes.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTypeProcessor;
import org.entando.kubernetes.model.bundle.processor.DirectoryProcessor;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.bundle.processor.FragmentProcessor;
import org.entando.kubernetes.model.bundle.processor.LabelProcessor;
import org.entando.kubernetes.model.bundle.processor.PageProcessor;
import org.entando.kubernetes.model.bundle.processor.PageTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.KubernetesService;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class AppConfigurationTest {

    @Test
    public void shouldDisablePageProcessor() {
        testPageProcessor(false);
    }

    @Test
    public void shouldEnablePageProcessor() {
        testPageProcessor(true);
    }

    private void testPageProcessor(boolean enabled) {
        AppConfiguration app = new AppConfiguration();
        app.setPageProcessorEnabled(enabled);

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansOfType(ComponentProcessor.class)).thenReturn(allProcessors());

        ComponentType[] expected = Arrays.stream(ComponentType.values())
                .filter(c -> c != ComponentType.PAGE || enabled)
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
        processors.put(ComponentType.LABEL.toString(), new LabelProcessor(coreClient));
        processors.put(ComponentType.PAGE.toString(), new PageProcessor(coreClient));
        processors.put(ComponentType.PAGE_TEMPLATE.toString(), new PageTemplateProcessor(coreClient));
        processors.put(ComponentType.PLUGIN.toString(), new PluginProcessor(k8sService));
        processors.put(ComponentType.WIDGET.toString(), new WidgetProcessor(coreClient));

        return processors;
    }

}
