package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.LanguageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.utils.TenantPrimaryContextJunitExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
@ExtendWith(TenantPrimaryContextJunitExt.class)
class LanguageProcessorTest extends BaseProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    @Mock
    private BundleReader bundleReader;

    private LanguageProcessor processor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new LanguageProcessor(engineService);
    }

    @Test
    void testEnableLanguages() throws IOException, ExecutionException, InterruptedException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        spec.setLanguages(singletonList("languages/languages.yaml"));

        List<LanguageDescriptor> languageDescriptorList = Arrays.asList(
                new LanguageDescriptor().setCode("en").setDescription("English"),
                new LanguageDescriptor().setCode("it").setDescription("Italiano"));

        when(bundleReader.readBundleDescriptor()).thenReturn(descriptor);
        when(bundleReader.readListOfDescriptorFile("languages/languages.yaml", LanguageDescriptor.class))
                .thenReturn(languageDescriptorList);

        final List<? extends Installable> installables = processor.process(bundleReader);

        assertThat(installables).hasSize(languageDescriptorList.size());
        IntStream.range(0, installables.size()).forEach(i -> {
            assertThat(installables.get(i)).isInstanceOf(LanguageInstallable.class);
            assertThat(installables.get(i).getComponentType()).isEqualTo(ComponentType.LANGUAGE);
            assertThat(installables.get(i).getName()).isEqualTo(languageDescriptorList.get(i).getCode());
        });

        verify(engineService, times(0)).enableLanguage(any());
        for (Installable<LanguageDescriptor> installable : installables) {
            installable.install().get();
        }

        final ArgumentCaptor<LanguageDescriptor> captor = ArgumentCaptor.forClass(LanguageDescriptor.class);
        verify(engineService, times(2)).enableLanguage(captor.capture());

        final List<LanguageDescriptor> languageDescriptorList1 = captor.getAllValues()
                .stream().sorted(Comparator.comparing(langDescriptor -> langDescriptor.getCode().toLowerCase()))
                .collect(Collectors.toList());

        assertThat(languageDescriptorList1).hasSameSizeAs(languageDescriptorList);
        IntStream.range(0, languageDescriptorList1.size()).forEach(i -> {
            assertThat(languageDescriptorList1.get(i).getCode()).isEqualTo(languageDescriptorList.get(i).getCode());
            assertThat(languageDescriptorList1.get(i).getDescription())
                    .isEqualTo(languageDescriptorList.get(i).getDescription());
        });
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new LanguageProcessor(new EntandoCoreClientTestDouble()), "language");
    }
}
