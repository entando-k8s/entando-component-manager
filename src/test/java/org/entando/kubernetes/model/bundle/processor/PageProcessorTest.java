package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PageProcessorTest extends BaseProcessorTest {

    private final String pageDescriptorFile = "/pages/my-page.yaml";

    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;

    private PageDescriptorValidator validator = new PageDescriptorValidator();

    @Mock
    private BundleReader bundleReader;

    private PageProcessor pageProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        validator.setupValidatorConfiguration();
        pageProcessor = new PageProcessor(entandoCoreClient, validator);
        lenient().when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
    }

    @Test
    void shouldReturnAListOfInstallablePagesFromTheBundle() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        initBundleReader(descriptor);
        List<Installable<PageDescriptor>> installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageInstallable.class);
        PageInstallable pginst = (PageInstallable) installables.get(0);
        assertThat(pginst.getName()).isEqualTo("my-page");

        descriptor.setName("name");
        descriptor.setCode(null);
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0).getRepresentation().getCode()).startsWith("name");

        descriptor.setCode(null);
        descriptor.setParentName("parent");
        descriptor.setParentCode(null);
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0).getRepresentation().getParentCode()).startsWith("parent");

        descriptor.setCode(null);
        descriptor.setParentName("parent");
        descriptor.setParentCode(null);
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        descriptor.setWidgets(Collections.singletonList(WidgetConfigurationDescriptor.builder()
                        .pos(0).name("my-name").build()));
        initBundleReader(descriptor);
        installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        PageDescriptor representation = installables.get(0).getRepresentation();
        assertThat(representation.getWidgets()).hasSize(1);
        assertThat(representation.getWidgets().get(0).getCode()).isNotEqualTo("my-name");
        assertThat(representation.getWidgets().get(0).getCode()).startsWith("my-name");
    }

    @Test
    void shouldThrowExceptionWhileProcessingPageAndValidationFails() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        descriptor.setName("name");
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        assertThrows(InvalidBundleException.class, () -> pageProcessor.process(bundleReader));

        descriptor.setCode(null);
        descriptor.setParentCode("parent");
        initBundleReader(descriptor);
        assertThrows(InvalidBundleException.class, () -> pageProcessor.process(bundleReader));

        descriptor.setParentCode("parent-12345678");
        descriptor.setParentName("parent");
        assertThrows(InvalidBundleException.class, () -> pageProcessor.process(bundleReader));
    }

    private void initBundleReader(PageDescriptor pageDescriptor) throws IOException {
        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPages(Collections.singletonList(pageDescriptorFile));
        when(bundleReader.readDescriptorFile(pageDescriptorFile, PageDescriptor.class))
                .thenReturn(pageDescriptor);
        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        lenient().when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }

    private PageDescriptor createPageDescriptor() {
        Map<String, String> pageTitles = new HashMap<>();
        pageTitles.put("it", "La mia pagina");
        pageTitles.put("en", "My page");
        PageDescriptor pageDescriptor = PageDescriptor.builder()
                .code("my-page")
                .parentCode("homepage-12345678")
                .pageModel("service")
                .ownerGroup("administrators")
                .titles(pageTitles)
                .build();
        return pageDescriptor;
    }


    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new PageProcessor(new EntandoCoreClientTestDouble(), validator), "page");
    }

    @Test
    void shouldComposeAndSetPageCodeWhileReadingDescriptorKeys() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        descriptor.setName("name");
        descriptor.setCode(null);
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        final List<String> pageCodeList = pageProcessor.readDescriptorKeys(bundleReader, pageDescriptorFile,
                pageProcessor);

        assertThat(pageCodeList).hasSize(1);
        assertThat(pageCodeList.get(0)).isEqualTo("name_77b2b10e");
    }
}
