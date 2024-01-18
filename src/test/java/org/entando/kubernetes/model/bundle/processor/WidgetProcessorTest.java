package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.TYPE_WIDGET_APPBUILDER;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.plugin.PluginVariable;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.templating.WidgetTemplateGeneratorServiceDouble;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.entando.kubernetes.validator.descriptor.WidgetDescriptorValidator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WidgetProcessorTest extends BaseProcessorTest {

    @Mock
    private BundleReader bundleReader;
    @Mock
    private WidgetDescriptorValidator validator;
    @Mock
    private ComponentDataRepository componentDataRepository;
    @Mock
    private KubernetesService kubernetesService;

    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final Map<String, String> pluginIngressPathMap = WidgetStubHelper.stubPluginIngressPathMap();
    private static final String STANDARD_WIDGET_DESCRIPTOR = "widgets/my_widget_descriptor.yaml";

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new WidgetProcessor(componentDataRepository, new EntandoCoreClientTestDouble(),
                        new WidgetTemplateGeneratorServiceDouble(), kubernetesService,
                        validator), "widget");
    }

    @Test
    void canProcessDescriptorV1() throws IOException {

        String widgDescrFile = "src/test/resources/bundle/widgets/my_widget_descriptor.yaml";
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);

        var installableList = execWidgetProcessor(widgDescrFile, null, bundleDescriptor);

        assertThat(installableList).hasSize(1);

        WidgetDescriptor actual = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widgDescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget");

        assertOnWidgetDescriptors(actual, expected);

        assertThat(actual.getCustomUi()).isEqualTo("<h2>Bundle 1 Widget</h2>");
        assertThat(actual.getConfigUi()).isNull();
        assertThat(actual.getCustomUiPath()).isNull();
    }

    @Test
    void canProcessDescriptorV5() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(bundleReader.getCode()).thenReturn(BundleStubHelper.BUNDLE_CODE);
        String widgetConfigFolder = "src/test/resources/bundle-v5/widgets/my_widget_config_descriptor_v5";
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "js")).thenReturn(
                List.of(
                        "widgets/my_widget_config_descriptor_v5/static/js/js-res-2.js",
                        "widgets/my_widget_config_descriptor_v5/static/js-res-1.js"
                )
        );
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "css")).thenReturn(
                List.of("widgets/my_widget_config_descriptor_v5/assets/css-res.css")
        );

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());

        String widgConfigDescrFile = "src/test/resources/bundle-v5/widgets/my_widget_config_descriptor_v5.yaml";
        String widgDescrFile = "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml";

        var installableList = execWidgetProcessor(widgDescrFile, widgConfigDescrFile,
                bundleDescriptor);

        assertThat(installableList).hasSize(1);

        WidgetDescriptor actual = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widgDescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget-77b2b10e");
        expected.setDescriptorMetadata(DescriptorMetadata.builder().bundleCode(BundleStubHelper.BUNDLE_CODE).build());

        assertOnWidgetDescriptors(actual, expected);

        assertThat(actual.getConfigWidget()).isNull();
        assertThat(actual.getApiClaims()).hasSize(expected.getApiClaims().size());
        assertThat(actual.getCustomElement()).isEqualTo("my-widget");
        assertThat(actual.getCustomUi()).isEqualTo(WidgetTemplateGeneratorServiceDouble.FTL_TEMPLATE);

        assertThat(actual.getConfigUi().getCustomElement()).isEqualTo("my-widget-config");
        List<String> res = actual.getConfigUi().getResources();
        assertThat(res).hasSize(3)
                .contains(
                        "bundles/" + BundleStubHelper.BUNDLE_CODE
                                + "/widgets/my_widget_config_descriptor_v5-77b2b10e/static/js/js-res-2.js")
                .contains(
                        "bundles/" + BundleStubHelper.BUNDLE_CODE
                                + "/widgets/my_widget_config_descriptor_v5-77b2b10e/static/js-res-1.js")
                .contains(
                        "bundles/" + BundleStubHelper.BUNDLE_CODE
                                + "/widgets/my_widget_config_descriptor_v5-77b2b10e/assets/css-res.css");
    }

    @Test
    void canProcess_WdigetAppBuilder_DescriptorV5() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        String bundleCode = "bundle-v5";
        when(bundleReader.getCode()).thenReturn(bundleCode);
        String widgetConfigFolder = "src/test/resources/bundle-v5/widgets/my_widget_config_descriptor_v5";
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "js")).thenReturn(
                List.of(
                        "widgets/my_widget_config_descriptor_v5/static/js/js-res-2.js",
                        "widgets/my_widget_config_descriptor_v5/static/js-res-1.js"
                )
        );
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "css")).thenReturn(
                List.of("widgets/my_widget_config_descriptor_v5/assets/css-res.css")
        );
        String widgConfigDescrFile =
                "src/test/resources/" + bundleCode + "/widgets/my_widget_config_descriptor_v5.yaml";
        String widgDescrFile = "src/test/resources/" + bundleCode + "/widgets/my_widget_app_builder_descriptor_v5.yaml";
        when(bundleReader.calculateBundleId()).thenReturn(
                BundleUtilities.removeProtocolAndGetBundleId(BundleInfoStubHelper.GIT_REPO_ADDRESS));

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList(STANDARD_WIDGET_DESCRIPTOR));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);

        var installableList = execWidgetProcessor(widgDescrFile, widgConfigDescrFile, bundleDescriptor);
        assertThat(installableList).hasSize(1);
        assertThat(installableList.get(0).getRepresentation().getExt()).isNotNull();
        assertThat(installableList.get(0).getRepresentation().getType()).isEqualTo(TYPE_WIDGET_APPBUILDER);
        assertThat(installableList.get(0).getRepresentation().getDescriptorMetadata().getAssets()).isEmpty();
    }

    private List<Installable<WidgetDescriptor>> execWidgetProcessor(String widgetDescFile, String widgConfigDescrFile,
            BundleDescriptor bundleDescriptor) throws IOException {
        //~
        WidgetDescriptor descriptor = yamlMapper.readValue(new File(widgetDescFile), WidgetDescriptor.class);

        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.readDescriptorFile(any(), any())).thenReturn(descriptor);
        when(validator.validateOrThrow(any())).thenReturn(true);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);
        widgetProcessor.setPluginIngressPathMap(pluginIngressPathMap);

        if (widgConfigDescrFile != null) {
            WidgetDescriptor wcdesc = yamlMapper.readValue(new File(widgConfigDescrFile), WidgetDescriptor.class);
            final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());
            wcdesc.setDescriptorMetadata(new DescriptorMetadata(
                    Map.of("", ""),
                    widgConfigDescrFile,
                    bundleDescriptor.getCode(),
                    null, null,
                    bundleId,
                    widgetProcessor.getTemplateGeneratorService()
            ));
            var wcdm = new HashMap<String, WidgetDescriptor>();
            wcdm.put(wcdesc.getName(), wcdesc);
            widgetProcessor.setWidgetConfigDescriptorsMap(wcdm);
        }

        return widgetProcessor.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    private void assertOnWidgetDescriptors(WidgetDescriptor actual, WidgetDescriptor expected) {
        assertThat(actual.getCode()).isEqualTo(expected.getCode());
        assertThat(actual.getTitles()).containsAllEntriesOf(expected.getTitles());
        assertThat(actual.getGroup()).isEqualTo(expected.getGroup());
        assertThat(actual.getDescriptorMetadata().getBundleCode()).isEqualTo(
                expected.getDescriptorMetadata().getBundleCode());
        assertThat(actual.getDescriptorMetadata().getPluginIngressPathMap()).isEqualTo(pluginIngressPathMap);
    }

    @Test
    void shouldNotAddBundleIdToWidgetCodeWhileComposingReportablesInBundleV1() throws IOException {
        final List<String> widgetsToProcess = Collections.singletonList("widget_descriptor_v1.yaml");
        ComponentSpecDescriptor componentSpecDescriptor = new ComponentSpecDescriptor().setWidgets(widgetsToProcess);
        when(bundleReader.readBundleDescriptor()).thenReturn(
                BundleStubHelper.stubBundleDescriptor(componentSpecDescriptor));

        WidgetDescriptor widgetDescriptor1 = new WidgetDescriptor().setCode(BundleStubHelper.BUNDLE_CODE);
        when(bundleReader.readDescriptorFile("widget_descriptor_v1.yaml", WidgetDescriptor.class))
                .thenReturn(widgetDescriptor1);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);

        final Reportable reportable = widgetProcessor.getReportable(bundleReader, widgetProcessor);

        assertThat(reportable.getCodes()).hasSize(1);
        assertThat(reportable.getCodes().get(0)).isEqualTo(
                BundleStubHelper.BUNDLE_CODE);
    }

    @Test
    void shouldAddBundleIdToWidgetCodeWhileComposingReportablesInBundleV5() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final List<String> widgetsToProcess = Collections.singletonList("widget_descriptor_v5.yaml");
        ComponentSpecDescriptor componentSpecDescriptor = new ComponentSpecDescriptor().setWidgets(widgetsToProcess);
        when(bundleReader.readBundleDescriptor()).thenReturn(
                BundleStubHelper.stubBundleDescriptor(componentSpecDescriptor));

        WidgetDescriptor widgetDescriptor5 = new WidgetDescriptor().setName(BundleStubHelper.BUNDLE_CODE);
        widgetDescriptor5.setDescriptorVersion("v5");
        when(bundleReader.readDescriptorFile("widget_descriptor_v5.yaml", WidgetDescriptor.class))
                .thenReturn(widgetDescriptor5);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);

        final Reportable reportable = widgetProcessor.getReportable(bundleReader, widgetProcessor);

        assertThat(reportable.getCodes()).hasSize(1);
        assertThat(reportable.getCodes().get(0)).isEqualTo(
                BundleStubHelper.BUNDLE_CODE + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldAddWidgetParentCodeWhileProcessingWidgetsWithParentName() throws IOException {

        when(bundleReader.calculateBundleId()).thenReturn(
                BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5()
                .setParentName(WidgetStubHelper.PARENT_NAME);

        List<Installable<WidgetDescriptor>> installableList = execWidgetProcessorParentNameParentCode(widgetDescriptor);
        final WidgetDescriptor actual = installableList.get(0).getRepresentation();

        assertThat(actual.getParentName()).isEqualTo(WidgetStubHelper.PARENT_NAME);
        assertThat(actual.getParentCode()).isEqualTo(
                WidgetStubHelper.PARENT_NAME + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldUseTheReceivedWidgetParentCodeIfAvailableWhileProcessingWidgets() throws IOException {

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5()
                .setParentCode(WidgetStubHelper.PARENT_NAME + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        List<Installable<WidgetDescriptor>> installableList = execWidgetProcessorParentNameParentCode(widgetDescriptor);
        final WidgetDescriptor actual = installableList.get(0).getRepresentation();

        assertThat(actual.getParentCode()).isEqualTo(
                WidgetStubHelper.PARENT_NAME + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldThrowExceptionWhileProcessingWidgetsAndValidationFails() throws IOException {

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5()
                .setParentCode(WidgetStubHelper.PARENT_NAME + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.readDescriptorFile(any(), any())).thenReturn(widgetDescriptor);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(validator.validateOrThrow(any())).thenThrow(InvalidBundleException.class);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);
        final InstallPlan installPlan = new InstallPlan();

        assertThrows(InvalidBundleException.class, () -> widgetProcessor.process(bundleReader, InstallAction.CREATE,
                installPlan));
    }

    @Test
    void canProcessDescriptorV5WithFTL() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(bundleReader.readFileAsString(any())).thenReturn("content");

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor_v5_with_custom_ui_path.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);

        String widgDescrFile = "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5_with_custom_ui_path.yaml";
        var installableList = execWidgetProcessor(widgDescrFile, null, bundleDescriptor);

        assertThat(installableList).hasSize(1);

        WidgetDescriptor actual = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widgDescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget-77b2b10e");

        assertThat(actual.getCustomUiPath()).isEqualTo("widget.ftl");
        assertThat(actual.getConfigUi()).isNull();
        assertThat(actual.getCustomUi()).isEqualTo("content");
    }

    private List<Installable<WidgetDescriptor>> execWidgetProcessorParentNameParentCode(
            WidgetDescriptor widgetDescriptor) throws IOException {

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec);

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.readDescriptorFile(any(), any())).thenReturn(widgetDescriptor);
        when(validator.validateOrThrow(any())).thenReturn(true);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);
        widgetProcessor.setPluginIngressPathMap(pluginIngressPathMap);

        return widgetProcessor.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }


    @Test
    void canProcessCorrectInstallationOrderV5() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(bundleReader.getCode()).thenReturn(BundleStubHelper.BUNDLE_CODE);
        String widgetConfigFolder = "src/test/resources/bundle-v5/widgets/my_widget_config_descriptor_v5";
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "js")).thenReturn(
                List.of(
                        "widgets/my_widget_config_descriptor_v5/static/js/js-res-2.js",
                        "widgets/my_widget_config_descriptor_v5/static/js-res-1.js"
                )
        );
        when(bundleReader.getWidgetResourcesOfType(widgetConfigFolder, "css")).thenReturn(
                List.of("widgets/my_widget_config_descriptor_v5/assets/css-res.css")
        );

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();

        // note this: first the logical widget, then the parent
        spec.setWidgets(List.of("widgets/my_logical_widget_descriptor_v5.yaml",
                "widgets/my_widget_descriptor_v5.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());

        final String widg1ConfigDescrFile = "src/test/resources/bundle-v5/widgets/my_widget_config_descriptor_v5.yaml";
        final String widg1DescrFile = "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml";
        final String widg2DescrFile = "src/test/resources/bundle-v5/widgets/my_logical_widget_descriptor_v5.yaml";

        WidgetDescriptor descriptor1 = yamlMapper.readValue(new File(widg1DescrFile), WidgetDescriptor.class);
        WidgetDescriptor descriptor2 = yamlMapper.readValue(new File(widg2DescrFile), WidgetDescriptor.class);

        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.readDescriptorFile(eq("widgets/my_widget_descriptor_v5.yaml"), any())).thenReturn(
                descriptor1);
        when(bundleReader.readDescriptorFile(eq("widgets/my_logical_widget_descriptor_v5.yaml"), any())).thenReturn(
                descriptor2);
        when(validator.validateOrThrow(any())).thenReturn(true);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator);
        widgetProcessor.setPluginIngressPathMap(pluginIngressPathMap);

        WidgetDescriptor wcdesc = yamlMapper.readValue(new File(widg1ConfigDescrFile), WidgetDescriptor.class);
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());
        wcdesc.setDescriptorMetadata(new DescriptorMetadata(
                Map.of("", ""),
                widg1ConfigDescrFile,
                bundleDescriptor.getCode(),
                null, null,
                bundleId,
                widgetProcessor.getTemplateGeneratorService()
        ));
        var wcdm = new HashMap<String, WidgetDescriptor>();
        wcdm.put(wcdesc.getName(), wcdesc);
        widgetProcessor.setWidgetConfigDescriptorsMap(wcdm);

        var installableList = widgetProcessor.process(bundleReader, InstallAction.CREATE, new InstallPlan());
        assertThat(installableList).hasSize(2);

        // the head should be the non-logical widget
        final WidgetDescriptor widget = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widg1DescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget-77b2b10e");
        expected.setDescriptorMetadata(DescriptorMetadata.builder().bundleCode(BundleStubHelper.BUNDLE_CODE).build());

        assertOnWidgetDescriptors(widget, expected);

        final WidgetDescriptor logicWidget = installableList.get(1).getRepresentation();
        expected = yamlMapper.readValue(new File(widg2DescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_logic_widget-77b2b10e");
        expected.setDescriptorMetadata(DescriptorMetadata.builder().bundleCode(BundleStubHelper.BUNDLE_CODE).build());

        assertOnWidgetDescriptors(logicWidget, expected);

    }

    @Test
    void showProcessPluginVariablesWithEmptyApiClaims() {
        WidgetDescriptor descriptor = new WidgetDescriptor();

        new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator)
                .processPluginVariables(descriptor);

        assertThat(descriptor.getApiClaims()).isNullOrEmpty();
    }

    @Test
    void shouldProcessPluginVariables() {
        final String pluVar1 = "my-reg";
        final String pluVar2 = "my-org";
        final String pluVar3 = "my-name";
        final String pluVar4 = "your-org";

        when(kubernetesService.resolvePluginsVariables(any())).thenReturn(List.of(
                new PluginVariable(WidgetStubHelper.API_CLAIM_2_NAME, WidgetStubHelper.PLUGIN_VARIABLE_1, pluVar1),
                new PluginVariable(WidgetStubHelper.API_CLAIM_2_NAME, WidgetStubHelper.PLUGIN_VARIABLE_2, pluVar2),
                new PluginVariable(WidgetStubHelper.API_CLAIM_2_NAME, WidgetStubHelper.PLUGIN_VARIABLE_3, pluVar3),
                new PluginVariable(WidgetStubHelper.API_CLAIM_3_NAME, WidgetStubHelper.PLUGIN_VARIABLE_4, pluVar4)));

        WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5()
                .setApiClaims(WidgetStubHelper.stubApiClaimsWithBundleReference());

        new WidgetProcessor(componentDataRepository,
                new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), kubernetesService, validator)
                .processPluginVariables(descriptor);

        final Map<String, String> apiClaimToBundleId = Map.of(
                WidgetStubHelper.API_CLAIM_3_NAME, "60ae211f",
                WidgetStubHelper.API_CLAIM_2_NAME, "f2a30ee7");

        final Map<String, String> apiClaimToBundleReference = Map.of(
                WidgetStubHelper.API_CLAIM_3_NAME, "your-org/entando-ms",
                WidgetStubHelper.API_CLAIM_2_NAME, "my-reg/my-org/my-name");

        descriptor.getApiClaims().forEach(ac -> {
            String bundleId = apiClaimToBundleId.getOrDefault(ac.getName(), null);
            String bundleReference = apiClaimToBundleReference.getOrDefault(ac.getName(), null);

            assertThat(ac.getBundleId()).isEqualTo(bundleId);
            assertThat(ac.getBundleReference()).isEqualTo(bundleReference);
        });
    }
}
