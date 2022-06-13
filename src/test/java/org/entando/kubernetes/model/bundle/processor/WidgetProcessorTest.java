package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
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

    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final Map<String, String> pluginIngressPathMap = WidgetStubHelper.stubPluginIngressPathMap();

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new WidgetProcessor(new EntandoCoreClientTestDouble(), new WidgetTemplateGeneratorServiceDouble(),
                        validator), "widget");
    }

    @Test
    void canProcessDescriptorV1() throws IOException {

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        String widgDescrFile = "src/test/resources/bundle/widgets/my_widget_descriptor.yaml";

        final List<Installable<WidgetDescriptor>> installableList = execWidgetProcessor(widgDescrFile);

        assertThat(installableList).hasSize(1);

        WidgetDescriptor actual = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widgDescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget-77b2b10e");

        assertOnWidgetDescriptors(actual, expected);

        assertThat(actual.getCustomUi()).isEqualTo("<h2>Bundle 1 Widget</h2>");
        assertThat(actual.getConfigUi()).isNull();
        assertThat(actual.getCustomUiPath()).isNull();
    }

    @Test
    void canProcessDescriptorV5() throws IOException {

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        String widgDescrFile = "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml";

        final List<Installable<WidgetDescriptor>> installableList = execWidgetProcessor(widgDescrFile);

        assertThat(installableList).hasSize(1);

        WidgetDescriptor actual = installableList.get(0).getRepresentation();
        WidgetDescriptor expected = yamlMapper.readValue(new File(widgDescrFile), WidgetDescriptor.class);
        expected.setCode("todomvc_widget-77b2b10e");

        assertOnWidgetDescriptors(actual, expected);

        assertThat(actual.getConfigWidget()).isNull();
        assertThat(actual.getApiClaims()).hasSize(expected.getApiClaims().size());
        assertThat(actual.getCustomElement()).isEqualTo("my-widget");
        assertThat(actual.getCustomUi()).isEqualTo(WidgetTemplateGeneratorServiceDouble.FTL_TEMPLATE);
    }

    private List<Installable<WidgetDescriptor>> execWidgetProcessor(String widgetDescFile) throws IOException {
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setWidgets(singletonList("widgets/my_widget_descriptor.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec);

        WidgetDescriptor descriptor = yamlMapper.readValue(new File(widgetDescFile), WidgetDescriptor.class);

        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.readDescriptorFile(any(), any())).thenReturn(descriptor);
        when(validator.validateOrThrow(any())).thenReturn(true);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), validator);
        widgetProcessor.setPluginIngressPathMap(pluginIngressPathMap);

        return widgetProcessor.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    private void assertOnWidgetDescriptors(WidgetDescriptor actual, WidgetDescriptor expected) {
        assertThat(actual.getCode()).isEqualTo(expected.getCode());
        assertThat(actual.getTitles()).containsAllEntriesOf(expected.getTitles());
        assertThat(actual.getGroup()).isEqualTo(expected.getGroup());
        assertThat(actual.getBundleId()).isEqualTo(BundleStubHelper.BUNDLE_CODE);
        assertThat(actual.getDescriptorMetadata().getPluginIngressPathMap()).isEqualTo(pluginIngressPathMap);
    }

    @Test
    void shouldAddWidgetCodeWhileComposingReportables() throws IOException {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final List<String> widgetsToProcess = List.of("widget_descriptor_v1.yaml", "widget_descriptor_v5.yaml");
        ComponentSpecDescriptor componentSpecDescriptor = new ComponentSpecDescriptor().setWidgets(widgetsToProcess);
        when(bundleReader.readBundleDescriptor()).thenReturn(
                BundleStubHelper.stubBundleDescriptor(componentSpecDescriptor));

        WidgetDescriptor widgetDescriptor1 = new WidgetDescriptor().setCode(BundleStubHelper.BUNDLE_CODE);
        when(bundleReader.readDescriptorFile("widget_descriptor_v1.yaml", WidgetDescriptor.class))
                .thenReturn(widgetDescriptor1);

        WidgetDescriptor widgetDescriptor5 = new WidgetDescriptor().setName(BundleStubHelper.BUNDLE_CODE);
        widgetDescriptor5.setDescriptorVersion("v5");
        when(bundleReader.readDescriptorFile("widget_descriptor_v5.yaml", WidgetDescriptor.class))
                .thenReturn(widgetDescriptor5);

        final WidgetProcessor widgetProcessor = new WidgetProcessor(new EntandoCoreClientTestDouble(),
                new WidgetTemplateGeneratorServiceDouble(), validator);

        final Reportable reportable = widgetProcessor.getReportable(bundleReader, widgetProcessor);

        assertThat(reportable.getCodes()).hasSize(2);
        assertThat(reportable.getCodes().get(0)).isEqualTo(
                BundleStubHelper.BUNDLE_CODE + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertThat(reportable.getCodes().get(1)).isEqualTo(
                BundleStubHelper.BUNDLE_CODE + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }
}

