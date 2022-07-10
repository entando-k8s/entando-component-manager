package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
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
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PageProcessorTest extends BaseProcessorTest {

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
    }

    @Test
    public void shouldReturnAListOfInstallablePagesFromTheBundle() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        initBundleReader(descriptor);
        List<? extends Installable> installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageInstallable.class);
        PageInstallable pginst = (PageInstallable) installables.get(0);
        assertThat(pginst.getName()).isEqualTo("my-page");
    }
    
    @Test
    public void shouldThrowExceptionWhileProcessingPageAndValidationFails() throws IOException {
        PageDescriptor descriptor = this.createPageDescriptor();
        descriptor.setName("name");
        descriptor.setDescriptorVersion("V5");
        initBundleReader(descriptor);
        assertThrows(InvalidBundleException.class, () -> pageProcessor.process(bundleReader));
    }
    
    private void initBundleReader(PageDescriptor pageDescriptor) throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPages(Collections.singletonList("/pages/my-page.yaml"));
        when(bundleReader.readDescriptorFile("/pages/my-page.yaml", PageDescriptor.class))
                .thenReturn(pageDescriptor);
        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }
    
    private PageDescriptor createPageDescriptor() {
        Map<String, String> pageTitles = new HashMap<>();
        pageTitles.put("it", "La mia pagina");
        pageTitles.put("en", "My page");
        PageDescriptor pageDescriptor = PageDescriptor.builder()
                .code("my-page")
                .parentCode("homepage")
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
}
