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
import org.entando.kubernetes.model.bundle.descriptor.content.ContentAttribute;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ContentProcessorTest extends BaseProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;
    @Mock
    private BundleReader bundleReader;
    private ContentProcessor contentProcessor;

    @BeforeEach
    public void setup() {
        contentProcessor = new ContentProcessor(entandoCoreClient);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        final String fileName = "contents/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setContents(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new ContentProcessor(new EntandoCoreClientTestDouble()), spec, fileName);
    }

    @Test
    void shouldReturnAListOfInstallableContentFromTheBundle() throws IOException {

        String contentDescriptorFile = "/contents/cng102-descriptor.yaml";

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setContents(Collections.singletonList(contentDescriptorFile));

        ContentDescriptor contentDescriptor = MAPPER.readValue(
                readFromFile("bundle/contents/cng102-descriptor.yaml"),
                ContentDescriptor.class);

        when(bundleReader
                .readDescriptorFile(contentDescriptorFile, ContentDescriptor.class))
                .thenReturn(contentDescriptor);

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor()).thenReturn(descriptor);

        List<? extends Installable> installables = contentProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(ContentInstallable.class);

        ContentInstallable installable = (ContentInstallable) installables.get(0);
        assertThat(installable.getName()).isEqualTo("CNG102");

        ContentDescriptor representation = installable.getRepresentation();
        assertThat(representation.getTypeCode()).isEqualTo("CNG");
        assertThat(representation.getDescription()).isEqualTo("Interest 3 card title - 2nd banner");
        assertThat(representation.getMainGroup()).isEqualTo("free");
        assertThat(representation.getStatus()).isEqualTo("PUBLIC");
        assertThat(representation.getViewPage()).isEqualTo("news");
        assertThat(representation.getListModel()).isEqualTo("10022");
        assertThat(representation.getDefaultModel()).isEqualTo("10003");
        assertThat(representation.getCategories()).containsOnly("cat1", "cat2");

        assertThat(representation.getAttributes()).hasSize(3);

        ContentAttribute attribute1 = representation.getAttributes()[0];
        ContentAttribute attribute2 = representation.getAttributes()[1];
        ContentAttribute attribute3 = representation.getAttributes()[2];

        assertThat(attribute1.getCode()).isEqualTo("title");
        assertThat(attribute1.getValue()).isNull();

        assertThat(attribute2.getCode()).isEqualTo("subtitle");
        assertThat(attribute2.getValue()).isNull();

        assertThat(attribute3.getCode()).isEqualTo("descr");
        assertThat(attribute3.getValue()).isNull();
    }

}
