package org.entando.kubernetes.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.model.bundle.processor.CategoryProcessor;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentProcessor;
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
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator;
import org.entando.kubernetes.validator.descriptor.WidgetDescriptorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AppConfigurationTest {

    private EntandoCoreClientTestDouble entandoCoreClientTestDouble;
    private K8SServiceClientTestDouble k8SServiceClientTestDouble;
    private AppConfiguration appConfig;
    @MockBean
    private ComponentDataRepository componentDataRepository;
    @MockBean
    private CraneCommand craneCommand;
    @MockBean
    private ApplicationContext context;
    @MockBean
    private EntandoCoreClient coreClient;
    @MockBean
    private KubernetesService k8sService;
    @MockBean
    private WidgetTemplateGeneratorService templateGeneratorService;
    @MockBean
    private WidgetDescriptorValidator widgetDescriptorValidator;
    @MockBean
    private PageDescriptorValidator pageDescriptorValidator;
    @MockBean
    private PluginDataRepository pluginDataRepository;
    @MockBean
    private PluginDescriptorValidator pluginDescriptorValidator;

    @BeforeEach
    void setup() {
        this.entandoCoreClientTestDouble = new EntandoCoreClientTestDouble();
        this.k8SServiceClientTestDouble = new K8SServiceClientTestDouble();
        this.appConfig = new AppConfiguration(this.entandoCoreClientTestDouble, this.k8SServiceClientTestDouble);
    }

    @Test
    void testPageProcessor() {
        when(context.getBeansOfType(ComponentProcessor.class)).thenReturn(allProcessors());

        ComponentType[] expected = Arrays.stream(ComponentType.values()).toArray(ComponentType[]::new);

        assertThat(appConfig.processorMap(context).keySet().stream()).containsExactlyInAnyOrder(
                new ComponentType[]{ComponentType.CONTENT_TEMPLATE,
                        ComponentType.CONTENT_TYPE,
                        ComponentType.CONTENT,
                        ComponentType.ASSET,
                        ComponentType.DIRECTORY,
                        ComponentType.RESOURCE,
                        ComponentType.FRAGMENT,
                        ComponentType.LANGUAGE,
                        ComponentType.LABEL,
                        ComponentType.GROUP,
                        ComponentType.CATEGORY,
                        ComponentType.PAGE_TEMPLATE
                });
    }


    @SuppressWarnings("rawtypes")
    private Map<String, ComponentProcessor> allProcessors() {

        Map<String, ComponentProcessor> processors = new HashMap<>();
        processors.put(ComponentType.CONTENT_TEMPLATE.toString(), new ContentTemplateProcessor(coreClient));
        processors.put(ComponentType.CONTENT_TYPE.toString(), new ContentTypeProcessor(coreClient));
        processors.put(ComponentType.CONTENT.toString(), new ContentProcessor(coreClient));
        processors.put(ComponentType.ASSET.toString(), new AssetProcessor(coreClient));
        processors.put(ComponentType.DIRECTORY.toString(), new DirectoryProcessor(coreClient));
        processors.put(ComponentType.RESOURCE.toString(), new FileProcessor(coreClient));
        processors.put(ComponentType.FRAGMENT.toString(), new FragmentProcessor(coreClient));
        processors.put(ComponentType.LANGUAGE.toString(), new LanguageProcessor(coreClient));
        processors.put(ComponentType.LABEL.toString(), new LabelProcessor(coreClient));
        processors.put(ComponentType.GROUP.toString(), new GroupProcessor(coreClient));
        processors.put(ComponentType.CATEGORY.toString(), new CategoryProcessor(coreClient));
        processors.put(ComponentType.PAGE_TEMPLATE.toString(), new PageTemplateProcessor(coreClient));
        /*
        processors.put(ComponentType.PAGE.toString(), new PageProcessor(coreClient, pageDescriptorValidator));

        processors.put(ComponentType.PLUGIN.toString(),
                new PluginProcessor(k8sService, pluginDescriptorValidator, pluginDataRepository, craneCommand));
        processors.put(ComponentType.WIDGET.toString(),
                new WidgetProcessor(componentDataRepository, coreClient, templateGeneratorService,
                        widgetDescriptorValidator));
        processors.put(ComponentType.PAGE_CONFIGURATION.toString(), new PageConfigurationProcessor(coreClient, pageDescriptorValidator));
        */
        return processors;
    }


    @Test
    void reportableProcessorShouldContainTheRightElements() {

        ApplicationContext context = mock(ApplicationContext.class);

        Map<String, ReportableComponentProcessor> reportableComponentProcessorMap = allReportableProcessors();

        when(context.getBeansOfType(ReportableComponentProcessor.class)).thenReturn(reportableComponentProcessorMap);

        List<ReportableComponentProcessor> reportableComponentProcessors = appConfig
                .reportableComponentProcessorList(context);

        ReportableComponentProcessor[] expected = reportableComponentProcessorMap.values()
                .toArray(ReportableComponentProcessor[]::new);

        assertThat(reportableComponentProcessors).containsExactlyInAnyOrder(expected);
    }


    private Map<String, ReportableComponentProcessor> allReportableProcessors() {
        EntandoCoreClient coreClient = mock(EntandoCoreClient.class);
        KubernetesService k8sService = mock(KubernetesService.class);
        PluginDescriptorValidator pluginDescriptorValidator = mock(PluginDescriptorValidator.class);
        WidgetTemplateGeneratorService templateGeneratorService = mock(WidgetTemplateGeneratorService.class);
        WidgetDescriptorValidator widgetDescriptorValidator = mock(WidgetDescriptorValidator.class);
        PageDescriptorValidator pageDescriptorValidator = mock(PageDescriptorValidator.class);
        PluginDataRepository pluginDataRepository = mock(PluginDataRepository.class);

        Map<String, ReportableComponentProcessor> processors = new HashMap<>();
        processors.put(ComponentType.CONTENT_TEMPLATE.toString(), new ContentTemplateProcessor(coreClient));
        processors.put(ComponentType.CONTENT_TYPE.toString(), new ContentTypeProcessor(coreClient));
        processors.put(ComponentType.CONTENT.toString(), new ContentProcessor(coreClient));
        processors.put(ComponentType.ASSET.toString(), new AssetProcessor(coreClient));
        processors.put(ComponentType.DIRECTORY.toString(), new DirectoryProcessor(coreClient));
        processors.put(ComponentType.RESOURCE.toString(), new FileProcessor(coreClient));
        processors.put(ComponentType.FRAGMENT.toString(), new FragmentProcessor(coreClient));
        processors.put(ComponentType.LANGUAGE.toString(), new LanguageProcessor(coreClient));
        processors.put(ComponentType.LABEL.toString(), new LabelProcessor(coreClient));
        processors.put(ComponentType.PAGE.toString(), new PageProcessor(coreClient, pageDescriptorValidator));
        processors.put(ComponentType.PAGE_TEMPLATE.toString(), new PageTemplateProcessor(coreClient));
        processors.put(ComponentType.PLUGIN.toString(),
                new PluginProcessor(k8sService, pluginDescriptorValidator, pluginDataRepository, craneCommand));
        processors.put(ComponentType.WIDGET.toString(),
                new WidgetProcessor(componentDataRepository, coreClient, templateGeneratorService,
                        widgetDescriptorValidator));
        processors.put(ComponentType.GROUP.toString(), new GroupProcessor(coreClient));
        processors.put(ComponentType.CATEGORY.toString(), new CategoryProcessor(coreClient));

        return processors;
    }

    @Test
    void analysisReportStrategiesShouldContainTheRightElements() {

        Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStreategies = appConfig
                .analysisReportStrategies();
        Map<ReportableRemoteHandler, AnalysisReportFunction> expected = allAnalysisReportStrategies();

        expected.keySet()
                .forEach(key -> {
                    assertThat(analysisReportStreategies).containsKey(key);
                    assertThat(analysisReportStreategies.get(key)).isNotNull();
                });
    }


    private Map<ReportableRemoteHandler, AnalysisReportFunction> allAnalysisReportStrategies() {

        return Map.of(
                // ENGINE ANALYSIS
                ReportableRemoteHandler.ENTANDO_ENGINE,
                entandoCoreClientTestDouble::getEngineAnalysisReport,
                // CMS ANALYSIS
                ReportableRemoteHandler.ENTANDO_CMS,
                entandoCoreClientTestDouble::getCMSAnalysisReport,
                // K8S SERVICE ANALYSIS
                ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                k8SServiceClientTestDouble::getAnalysisReport
        );
    }
}
