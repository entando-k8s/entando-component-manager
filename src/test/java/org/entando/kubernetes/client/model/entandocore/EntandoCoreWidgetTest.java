package org.entando.kubernetes.client.model.entandocore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ConfigUi;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget.WidgetParameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntandoCoreWidgetTest {

    @Test
    public void shouldReadDescriptorWithoutConfigUi() {
        EntandoCoreWidget ecw = new EntandoCoreWidget(testWidgetDescriptor());
        assertThat(ecw.getBundleId()).isEqualTo("my-bundle");
        assertThat(ecw.getCode()).isEqualTo("my-code");
        assertThat(ecw.getCustomUi()).isEqualTo("<h1>Hello world</h1>");
        assertThat(ecw.getGroup()).isEqualTo("free");
        assertThat(ecw.getTitles().keySet()).containsExactlyInAnyOrder("it", "en");
        assertThat(ecw.getTitles().get("it")).isEqualTo("Il mio titolo");
        assertThat(ecw.getTitles().get("en")).isEqualTo("My title");
        assertThat(ecw.getConfigUi()).isNull();
        assertThat(ecw.getParameters()).isNull();
        assertThat(ecw.getParentType()).isNull();
        assertThat(ecw.getConfig()).isNull();
    }

    @Test
    public void shouldReadConfigUiDescriptor() {
        WidgetDescriptor wd = testWidgetDescriptor();
        wd.setConfigUi(testConfigUiDescriptor());

        EntandoCoreWidget ecw = new EntandoCoreWidget(wd);
        assertThat(ecw.getConfigUi()).isNotNull();
        assertThat(ecw.getConfigUi().keySet()).containsExactlyInAnyOrder("customElement", "resources");
        assertThat(ecw.getConfigUi().get("customElement")).isEqualTo("myCustomElement");
        assertThat(ecw.getConfigUi().get("resources")).isInstanceOf(List.class);

        List<String> resources = (List<String>) ecw.getConfigUi().get("resources");
        assertThat(resources).containsExactlyInAnyOrder("css/style.css", "js/main.js", "js/runtime.js");

        assertThat(ecw.getParameters()).isNull();
        assertThat(ecw.getParentType()).isNull();
        assertThat(ecw.getConfig()).isNull();
    }

    @Test
    public void shouldReadParametrizedType() {
        WidgetDescriptor wd = this.testWidgetDescriptor();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "description of key 1");
        parameters.put("key2", "description of key 2");
        wd.setParameters(parameters);

        EntandoCoreWidget ecw = new EntandoCoreWidget(wd);
        List<WidgetParameter> ecwParameters = (List<WidgetParameter>) ecw.getParameters();
        Assertions.assertEquals(2, ecwParameters.size());
        Assertions.assertEquals("key1", ecwParameters.get(0).getCode());
        Assertions.assertEquals("key2", ecwParameters.get(1).getCode());
        Assertions.assertEquals("description of key 1", ecwParameters.get(0).getDescription());
        Assertions.assertEquals("description of key 2", ecwParameters.get(1).getDescription());

        assertThat(ecw.getParentType()).isNull();
        assertThat(ecw.getConfig()).isNull();
    }

    @Test
    public void shouldReadLogicalType() {
        WidgetDescriptor wd = this.testWidgetDescriptor();
        wd.setParentType("parent_code");
        Map<String, String> config = new HashMap<>();
        config.put("key1", "Value1");
        config.put("key2", "Value2");
        config.put("key3", "Value3");
        wd.setConfig(config);

        EntandoCoreWidget ecw = new EntandoCoreWidget(wd);
        Map<String, String> ecwConfig = ecw.getConfig();
        Assertions.assertEquals(3, ecwConfig.size());
        Assertions.assertEquals("Value1", ecwConfig.get("key1"));
        Assertions.assertEquals("Value2", ecwConfig.get("key2"));
        Assertions.assertEquals("Value3", ecwConfig.get("key3"));
        Assertions.assertEquals("parent_code", ecw.getParentType());
        assertThat(ecw.getParameters()).isNull();
    }

    private ConfigUi testConfigUiDescriptor() {
        ConfigUi configUI = new ConfigUi();
        configUI.setCustomElement("myCustomElement");
        configUI.setResources(Arrays.asList("css/style.css", "js/main.js", "js/runtime.js"));

        return configUI;
    }

    private WidgetDescriptor testWidgetDescriptor() {

        Map<String, String> titles = new HashMap<>();
        titles.put("en", "My title");
        titles.put("it", "Il mio titolo");

        return WidgetDescriptor.builder()
                .code("my-code")
                .customUi("<h1>Hello world</h1>")
                .group("free")
                .titles(titles)
                .descriptorMetadata(DescriptorMetadata.builder().bundleCode("my-bundle").build())
                .build();
    }
}
