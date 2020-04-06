package org.entando.kubernetes.client.model.entandocore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor.ConfigUIDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
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

    }

    private ConfigUIDescriptor testConfigUiDescriptor() {
        ConfigUIDescriptor configUIDescriptor = new WidgetDescriptor.ConfigUIDescriptor();
        configUIDescriptor.setCustomElement("myCustomElement");
        configUIDescriptor.setResources(Arrays.asList("css/style.css", "js/main.js", "js/runtime.js"));

        return configUIDescriptor;
    }

    private WidgetDescriptor testWidgetDescriptor() {

        Map<String, String> titles = new HashMap<>();
        titles.put("en", "My title");
        titles.put("it", "Il mio titolo");

        WidgetDescriptor descriptor = new WidgetDescriptor();
        descriptor.setCode("my-code");
        descriptor.setBundleId("my-bundle");
        descriptor.setCustomUi("<h1>Hello world</h1>");
        descriptor.setGroup("free");
        descriptor.setTitles(titles);
        return descriptor;
    }
}
