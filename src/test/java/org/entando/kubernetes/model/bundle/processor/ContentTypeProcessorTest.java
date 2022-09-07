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
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeAttribute;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTypeInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class ContentTypeProcessorTest extends BaseProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;
    @Mock
    private BundleReader bundleReader;
    private ContentTypeProcessor contentTypeProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        contentTypeProcessor = new ContentTypeProcessor(entandoCoreClient);
    }

    @Test
    public void shouldReturnAListOfInstallableContentTypesFromTheBundle() throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setContentTypes(Collections.singletonList("/contenttypes/my_content_type_descriptor.yaml"));

        ContentTypeDescriptor contentTypeDescriptor = MAPPER.readValue(
                readFromFile("bundle/contenttypes/my_content_type_descriptor.yaml"),
                ContentTypeDescriptor.class);

        when(bundleReader
                .readDescriptorFile("/contenttypes/my_content_type_descriptor.yaml", ContentTypeDescriptor.class))
                .thenReturn(contentTypeDescriptor);

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor()).thenReturn(descriptor);

        List<? extends Installable> installables = contentTypeProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(ContentTypeInstallable.class);

        ContentTypeInstallable installable = (ContentTypeInstallable) installables.get(0);
        assertThat(installable.getName()).isEqualTo("CNG");
        assertThat(installable.getRepresentation().getAttributes()).hasSize(3);

        ContentTypeAttribute attribute1 = installable.getRepresentation().getAttributes().get(0);
        ContentTypeAttribute attribute2 = installable.getRepresentation().getAttributes().get(1);
        ContentTypeAttribute attribute3 = installable.getRepresentation().getAttributes().get(2);

        assertThat(attribute1.getCode()).isEqualTo("title");
        assertThat(attribute1.getType()).isEqualTo("Text");

        assertThat(attribute2.getCode()).isEqualTo("attachments");
        assertThat(attribute2.getType()).isEqualTo("Monolist");
        assertThat(attribute2.getNestedAttribute().getCode()).isEqualTo("attach");
        assertThat(attribute2.getNestedAttribute().getType()).isEqualTo("Attach");

        assertThat(attribute3.getCode()).isEqualTo("composite");
        assertThat(attribute3.getType()).isEqualTo("Composite");
        assertThat(attribute3.getCompositeAttributes()).hasSize(1);
        assertThat(attribute3.getCompositeAttributes().get(0).getCode()).isEqualTo("comp_img");
        assertThat(attribute3.getCompositeAttributes().get(0).getType()).isEqualTo("Image");
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        final String fileName = "content-types/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setContentTypes(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new ContentTypeProcessor(new EntandoCoreClientTestDouble()), spec, fileName);
    }

}
