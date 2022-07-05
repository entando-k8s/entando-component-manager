package org.entando.kubernetes.model.bundle.descriptor.widget;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.WidgetExt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class WidgetDescriptorTest {

    private static final String APP_BUILDER_JSON = String.format("{\"slot\":\"content\"}");
    private static final String ADMIN_CONSOLE_JSON = String.format("{}");
    private static final String ROOT_JSON = String.format("{\"appBuilder\":%s,\"adminConsole\":%s}", APP_BUILDER_JSON,
            ADMIN_CONSOLE_JSON);

    private static final String WIDGET_DESCRIPTOR_VERSION = DescriptorVersion.V5.getVersion();
    private static final String WIDGET_DESCRIPTOR_JSON = "{\"descriptorVersion\":\"" + WIDGET_DESCRIPTOR_VERSION
            + "\",\"code\":\"point2d-details-widget-64c57c33\",\"titles\":{\"en\":\"Point 2 D Details Widget\","
            + "\"it\":\"Point 2 D Details Widget\"},\"group\":\"free\",\"configUi\":{\"customElement\":\"point-2-d-details-config\","
            + "\"resources\":[]},\"customUiPath\":null,\"configWidget\":null,\"name\":\"point2d-details-widget\",\"type\":\"widget\","
            + "\"configMfe\":\"point2d-details-widget-config\",\"apiClaims\":null,\"params\":[{\"name\":\"url\",\"description\":"
            + "\"the url\"},{\"name\":\"title\",\"description\":\"the title\"}],\"contextParams\":[\"page_code\",\"info_startLang\","
            + "\"systemParam_applicationBaseURL\"],\"customElement\":\"point-2-d-details\",\"ext\":null,"
            + "\"descriptorMetadata\":{\"pluginIngressPathMap\":{},\"filename\":\"widgets/point2d-details-widget-descriptor.yaml\","
            + "\"bundleCode\":\"myapp2mysql-bundle-64c57c33\",\"assets\":null,\"systemParams\":{\"api\":{ \"ext-api\":{\"url\":\"http://test.com\"}}},"
            + "\"bundleId\":\"64c57c33\"},\"parentName\":null,\"parentCode\":null,\"componentKey\":{\"key\":\"point2d-details-widget-64c57c33\"},"
            + "\"auxiliary\":false,\"version1\":false,\"descriptorClassName\":\"WidgetDescriptor\",\"ext\":" + ROOT_JSON
            + "}";

    @Test
    void shouldNotDeserializeExtsubField()
            throws JsonProcessingException { // because it is set inside the PluginProcessor
        ObjectMapper mapper = new ObjectMapper();
        WidgetExt ext = mapper.readValue(ROOT_JSON, WidgetExt.class);
        assertThat(ext.getAppBuilder()).isEqualTo(APP_BUILDER_JSON);
        String output = mapper.writeValueAsString(ext);
        assertThat(output).isEqualTo(ROOT_JSON);
    }

    @Test
    void shouldDeserializeWidgetDescriptor() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        WidgetDescriptor descriptor = mapper.readValue(WIDGET_DESCRIPTOR_JSON, WidgetDescriptor.class);
        assertThat(descriptor.getDescriptorVersion()).isEqualTo(WIDGET_DESCRIPTOR_VERSION);
        assertThat(descriptor.getExt().getAppBuilder()).isEqualTo(APP_BUILDER_JSON);
    }

    @Test
    void should_isAuxiliary_WorkFine_WidgetDescriptor() {
        WidgetDescriptor descriptor = new WidgetDescriptor();

        descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        assertThat(descriptor.isAuxiliary()).isFalse();

        descriptor.setDescriptorVersion(null);
        assertThat(descriptor.isAuxiliary()).isFalse();

        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        assertThat(descriptor.isAuxiliary()).isFalse();

        descriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        descriptor.setType(WidgetDescriptor.TYPE_WIDGET_STANDARD);
        assertThat(descriptor.isAuxiliary()).isFalse();

        descriptor.setType(WidgetDescriptor.TYPE_WIDGET_CONFIG);
        assertThat(descriptor.isAuxiliary()).isTrue();

    }
}
