package org.entando.kubernetes.client.model.bundle.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.*;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageTemplateInstallable;
import org.entando.kubernetes.model.bundle.processor.PageTemplateProcessor;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.utils.FileUtils.readFromFile;
import static org.mockito.Mockito.when;

@Tag("unit")
public class PageTemplateProcessorTest {

    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;

    @Mock
    private BundleReader bundleReader;

    private PageTemplateProcessor pageTemplateProcessor;

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pageTemplateProcessor = new PageTemplateProcessor(entandoCoreClient);
    }

    @Test
    public void shouldReturnAListOfInstallableContentTypesFromTheBundle() throws IOException {
        final EntandoBundleJob job = new EntandoBundleJob();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPageModels(Collections.singletonList("/pagemodels/my_page_model_descriptor.yaml"));

        PageTemplateDescriptor pageTe = MAPPER.readValue(
                readFromFile("bundle/pagemodels/my_page_model_descriptor.yaml"),
                PageTemplateDescriptor.class);

        when(bundleReader.readDescriptorFile("/pagemodels/my_page_model_descriptor.yaml", PageTemplateDescriptor.class))
                .thenReturn(pageTe);

        BundleDescriptor descriptor = new BundleDescriptor("my-component", "desc", spec);
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
        FrameDescriptor breadCrumbFrame = installable.getRepresentation().getConfiguration().getFrames().get(1);
        FrameDescriptor bodyFrame = installable.getRepresentation().getConfiguration().getFrames().get(2);
        FrameDescriptor footerFrame = installable.getRepresentation().getConfiguration().getFrames().get(3);

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
        assertThat(breadCrumbFrame.getDefaultWidget().getProperties()).isNull();

        assertThat(bodyFrame.getDescription()).isEqualTo("Body");
        assertThat(bodyFrame.getDefaultWidget()).isNotNull();
        assertThat(bodyFrame.getDefaultWidget().getCode()).isEqualTo("my-widget");
        assertThat(bodyFrame.getDefaultWidget().getProperties()).isNotNull();
        assertThat(bodyFrame.getDefaultWidget().getProperties().get("title")).isEqualTo("My fantastic widget");

        assertThat(footerFrame.getDescription()).isEqualTo("Footer");
        assertThat(footerFrame.getDefaultWidget()).isNull();
    }
}
