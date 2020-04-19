package org.entando.kubernetes.client.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.PageProcessor;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
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
    public void shouldProcessPagesAndPageModels() {
        assertThat(pageProcessor.shouldProcess(ComponentType.PAGE)).isTrue();
        assertThat(pageProcessor.shouldProcess(ComponentType.PAGE_MODEL)).isTrue();
    }

    @Test
    public void shouldNotProcessOtherTypes() {
        assertThat(pageProcessor.shouldProcess(ComponentType.CONTENT_TYPE)).isFalse();
    }

    @Test
    public void shouldReturnAListOfInstallablePagesFromTheBundle() throws IOException {
        final DigitalExchangeJob job = new DigitalExchangeJob();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPages(Collections.singletonList("/pages/my-page.yaml"));

        Map<String, String> pageTitles = new HashMap<>();
        pageTitles.put("it", "La mia pagina");
        pageTitles.put("en", "My page");

        PageDescriptor pageDescriptor = new PageDescriptor();
        pageDescriptor.setCode("my-page");
        pageDescriptor.setParentCode("homepage");
        pageDescriptor.setCharset("utf-8");
        pageDescriptor.setDisplayedInMenu(true);
        pageDescriptor.setPageModel("service");
        pageDescriptor.setOwnerGroup("administrators");
        pageDescriptor.setSeo(false);
        pageDescriptor.setTitles(pageTitles);

        when(bundleReader.readDescriptorFile("/pages/my-page.yaml", PageDescriptor.class)).thenReturn(pageDescriptor);

        ComponentDescriptor descriptor = new ComponentDescriptor("my-component", "desc", spec);

        List<? extends Installable> installables = pageProcessor.process(job, bundleReader, descriptor);
        assertThat(installables).hasSize(1);
        assertThat(installables.get(0)).isInstanceOf(PageInstallable.class);
        PageInstallable pginst = (PageInstallable) installables.get(0);

        assertThat(pginst.getName()).isEqualTo("my-page");
    }


}
