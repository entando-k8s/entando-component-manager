package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.utils.FileUtils.readFromFile;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.SketchDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageTemplateInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PageTemplateProcessorTest extends BaseProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;
    @Mock
    private BundleReader bundleReader;
    private PageTemplateProcessor pageTemplateProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pageTemplateProcessor = new PageTemplateProcessor(entandoCoreClient);
    }

    @Test
    public void shouldReturnAListOfInstallableContentTypesFromTheBundle() throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPageTemplates(Collections.singletonList("/pagemodels/my_page_model_descriptor.yaml"));

        PageTemplateDescriptor pageTe = MAPPER.readValue(
                readFromFile("bundle/pagemodels/my_page_model_descriptor.yaml"),
                PageTemplateDescriptor.class);

        when(bundleReader.readDescriptorFile("/pagemodels/my_page_model_descriptor.yaml",
                PageTemplateDescriptor.class))
                .thenReturn(pageTe);

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor()).thenReturn(descriptor);

        List<? extends Installable> installables = pageTemplateProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageTemplateInstallable.class);

        PageTemplateInstallable installable = (PageTemplateInstallable) installables.get(0);
        assertThat(installable.getName()).isEqualTo("todomvc_page_model");

        PageTemplateDescriptor extractedDesciptor = installable.getRepresentation();

        assertThat(extractedDesciptor.getCode().equals("todomvc_page_model"));
        assertThat(installable.getRepresentation().getConfiguration().getFrames()).hasSize(4);

        FrameDescriptor headerFrame = installable.getRepresentation().getConfiguration().getFrames().get(0);
        final FrameDescriptor breadCrumbFrame = installable.getRepresentation().getConfiguration().getFrames().get(1);
        final FrameDescriptor bodyFrame = installable.getRepresentation().getConfiguration().getFrames().get(2);
        final FrameDescriptor footerFrame = installable.getRepresentation().getConfiguration().getFrames().get(3);

        assertThat(headerFrame.getDescription()).isEqualTo("Header");
        assertThat(headerFrame.getDefaultWidget()).isNull();
        SketchDescriptor expectedSketch = new SketchDescriptor();
        expectedSketch.setX1(0);
        expectedSketch.setX2(11);
        expectedSketch.setY1(0);
        expectedSketch.setY2(0);
        assertThat(headerFrame.getSketch()).isEqualToComparingFieldByField(expectedSketch);

        assertThat(breadCrumbFrame.getDescription()).isEqualTo("Breadcrumb");
        assertThat(breadCrumbFrame.getDefaultWidget()).isNotNull();
        assertThat(breadCrumbFrame.getDefaultWidget().getCode()).isEqualTo("breadcrumb");

        assertThat(bodyFrame.getDescription()).isEqualTo("Body");
        assertThat(bodyFrame.getDefaultWidget()).isNotNull();
        assertThat(bodyFrame.getDefaultWidget().getCode()).isEqualTo("my-widget");

        assertThat(footerFrame.getDescription()).isEqualTo("Footer");
        assertThat(footerFrame.getDefaultWidget()).isNull();
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        final String fileName = "page-templates/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPageTemplates(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new PageTemplateProcessor(new EntandoCoreClientTestDouble()), spec, fileName);
    }

}
