package org.entando.kubernetes.service.templating;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService.SystemParams;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class WidgetTemplateGeneratorServiceTest {

    private WidgetTemplateGeneratorService targetService;
    private PluginDataRepository pluginDataRepository;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private static final String INGRESS_PATH = "/ingress/path";

    @BeforeEach
    public void setup() {
        pluginDataRepository = Mockito.mock(PluginDataRepository.class);
        targetService = new WidgetTemplateGeneratorServiceImpl(pluginDataRepository);
    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void generateSystemParamsForConfig_shouldCreatePath() throws IOException {
        String widgDescriptorFile = "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml";
        WidgetDescriptor widgetDescriptor = yamlMapper.readValue(new File(widgDescriptorFile), WidgetDescriptor.class);

        when(pluginDataRepository.findByBundleIdAndPluginName(anyString(), anyString())).thenReturn(
                Optional.of(PluginDataEntity.builder().endpoint(INGRESS_PATH + "/").build()));
        SystemParams systemParams = targetService.generateSystemParamsWithIngressPath(widgetDescriptor.getApiClaims(),
                "bundle-id");
        assertThat(systemParams.getApi().get("ext-api").getUrl()).isEqualTo(INGRESS_PATH);
    }

}
