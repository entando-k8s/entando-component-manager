package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
public class PageProcessorTest {

    @Mock
    private DefaultEntandoCoreClient entandoCoreClient;

    @Mock
    private BundleReader bundleReader;

    private PageProcessor pageProcessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pageProcessor = new PageProcessor(entandoCoreClient);
    }

    @Test
    public void shouldReturnAListOfInstallablePagesFromTheBundle() throws IOException {
        initBundleReader();

        List<? extends Installable> installables = pageProcessor.process(bundleReader);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageInstallable.class);
        PageInstallable pginst = (PageInstallable) installables.get(0);

        assertThat(pginst.getName()).isEqualTo("my-page");
    }

    private void initBundleReader() throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPages(Collections.singletonList("/pages/my-page.yaml"));

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

        when(bundleReader.readDescriptorFile("/pages/my-page.yaml", PageDescriptor.class))
                .thenReturn(pageDescriptor);

        BundleDescriptor descriptor = new BundleDescriptor("my-component", "desc", spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }
}
