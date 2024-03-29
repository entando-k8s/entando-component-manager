package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
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
import org.entando.kubernetes.model.bundle.installable.PageConfigurationInstallable;
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
class PageConfigurationProcessorTest extends BaseProcessorTest {

    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;
    
    private PageDescriptorValidator validator = new PageDescriptorValidator();
    
    @Mock
    private BundleReader bundleReader;

    private PageConfigurationProcessor pageConfigurationProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        validator.setupValidatorConfiguration();
        pageConfigurationProcessor = new PageConfigurationProcessor(entandoCoreClient, validator);
        Mockito.lenient().when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
    }

    @Test
    void shouldReturnAListOfInstallablePagesFromTheBundle() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        initBundleReader(descriptor);

        List<Installable<PageDescriptor>> installables  = pageConfigurationProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageConfigurationInstallable.class);
        PageConfigurationInstallable pginst = (PageConfigurationInstallable) installables.get(0);

        assertThat(pginst.getName()).isEqualTo("my-page");
        
        descriptor.setName("name");
        descriptor.setCode(null);
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        installables = pageConfigurationProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0).getRepresentation().getCode()).startsWith("name");
        
        descriptor.setCode(null);
        descriptor.setParentName("parent");
        descriptor.setParentCode(null);
        initBundleReader(descriptor);
        installables = pageConfigurationProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0).getRepresentation().getParentCode()).startsWith("parent");
        
        descriptor.setCode(null);
        descriptor.setParentName("parent");
        descriptor.setParentCode(null);
        initBundleReader(descriptor);
        descriptor.getWidgets().add(WidgetConfigurationDescriptor.builder()
                        .pos(1).name("my-name").build());
        initBundleReader(descriptor);
        installables = pageConfigurationProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        PageDescriptor representation = installables.get(0).getRepresentation();
        assertThat(representation.getWidgets()).hasSize(2);
        assertThat(representation.getWidgets().get(0).getCode()).isEqualTo("my-code");
        assertThat(representation.getWidgets().get(1).getCode()).isNotEqualTo("my-name");
        assertThat(representation.getWidgets().get(1).getCode()).startsWith("my-name");
    }
    
    @Test
    void shouldThrowExceptionWhileProcessingPageAndValidationFails() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        descriptor.setName("name");
        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        initBundleReader(descriptor);
        assertThrows(InvalidBundleException.class, () -> pageConfigurationProcessor.process(bundleReader));
        
        descriptor.setCode(null);
        descriptor.setParentCode("parent");
        assertThrows(InvalidBundleException.class, () -> pageConfigurationProcessor.process(bundleReader));
        
        descriptor.setParentCode("parent-12345678");
        descriptor.setParentName("parent");
        assertThrows(InvalidBundleException.class, () -> pageConfigurationProcessor.process(bundleReader));
    }
    
    private PageDescriptor createPageDescriptor() {
        Map<String, String> pageTitles = new HashMap<>();
        pageTitles.put("it", "La mia pagina");
        pageTitles.put("en", "My page");
        List<WidgetConfigurationDescriptor> widgets = new ArrayList<>();
        widgets.add(WidgetConfigurationDescriptor.builder()
                        .pos(0).code("my-code").build());
        PageDescriptor pageConfigurationDescriptor = PageDescriptor.builder()
                .code("my-page")
                .parentCode("homepage-12345678")
                .charset("utf-8")
                .displayedInMenu(true)
                .pageModel("service")
                .ownerGroup("administrators")
                .seo(false)
                .titles(pageTitles)
                .widgets(widgets)
                .build();
        return pageConfigurationDescriptor;
    }

    private void initBundleReader(PageDescriptor pageConfigurationDescriptor) throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPages(Collections.singletonList("/pages/my-page.yaml"));

        when(bundleReader.readDescriptorFile("/pages/my-page.yaml", PageDescriptor.class))
                .thenReturn(pageConfigurationDescriptor);

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new PageConfigurationProcessor(new EntandoCoreClientTestDouble(), validator), "pageConfiguration");
    }
    
}
