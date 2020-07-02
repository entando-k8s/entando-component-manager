package org.entando.kubernetes.client.model.entandocore;

import org.entando.kubernetes.model.bundle.descriptor.DefaultWidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.SketchDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCorePageTemplate;
import org.entando.kubernetes.model.entandocore.EntandoCoreSketchDescriptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
public class EntandoCorePageTemplateTest {

    @Test
    public void shouldReadDescriptorFile() {
        EntandoCorePageTemplate ecpt = new EntandoCorePageTemplate(getTestPageTemplateDescriptor());
        assertThat(ecpt.getCode()).isEqualTo("my-page-template");
        assertThat(ecpt.getDescr()).isEqualTo("My page template");
        assertThat(ecpt.getTemplate()).isEqualTo("<html><body><h1>Hello World</h1></body></html>");
        assertThat(ecpt.getConfiguration().getFrames()).hasSize(1);
        assertThat(ecpt.getConfiguration().getFrames().get(0).getPos()).isEqualTo("0");
        assertThat(ecpt.getConfiguration().getFrames().get(0).getDescr()).isEqualTo("my-frame");
        assertThat(ecpt.getConfiguration().getFrames().get(0).isMainFrame()).isTrue();
        assertThat(ecpt.getConfiguration().getFrames().get(0).getSketch())
                .isEqualToComparingFieldByField(new EntandoCoreSketchDescriptor(0, 0, 11, 0));
        assertThat(ecpt.getConfiguration().getFrames().get(0).getDefaultWidget().getCode()).isEqualTo("default-widget");
        assertThat(ecpt.getConfiguration().getFrames().get(0).getDefaultWidget().getProperties().get("title")).isEqualTo("default");
        assertThat(ecpt.getTitles().keySet()).containsExactlyInAnyOrder("it", "en");
        assertThat(ecpt.getTitles().values()).containsExactlyInAnyOrder("Il mio template di pagina", "My page template");
    }

    private PageTemplateDescriptor getTestPageTemplateDescriptor() {
        Map<String, String> templateTitles = new HashMap<>();
        templateTitles.put("it", "Il mio template di pagina");
        templateTitles.put("en", "My page template");

        Map<String, String> properties = new HashMap<>();
        properties.put("title", "default");

        FrameDescriptor frame = FrameDescriptor.builder()
                .pos("0")
                .description("my-frame")
                .mainFrame(true)
                .sketch(new SketchDescriptor(0, 0, 11, 0))
                .defaultWidget(DefaultWidgetDescriptor.builder()
                        .code("default-widget")
                        .properties(properties)
                        .build())
                .build();

        PageTemplateConfigurationDescriptor configuration = PageTemplateConfigurationDescriptor.builder()
                .frames(Collections.singletonList(frame)).build();

        return PageTemplateDescriptor.builder()
                .code("my-page-template")
                .titles(templateTitles)
                .description("My page template")
                .template("<html><body><h1>Hello World</h1></body></html>")
                .configuration(configuration)
                .build();
    }
}
