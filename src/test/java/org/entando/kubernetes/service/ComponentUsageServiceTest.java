package org.entando.kubernetes.service;

import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Tag("in-process")
public class ComponentUsageServiceTest {

    private EntandoBundleComponentUsageService usageService;
    private EntandoCoreClient client;

    @BeforeEach
    public void setup() {
        client = Mockito.mock(EntandoCoreClient.class);
        this.usageService = new EntandoBundleComponentUsageService(client);
    }

    @Test
    public void shouldReturnComponentUsageForValidComponent() {
        when(client.getWidgetUsage(eq("my-widget")))
                .thenReturn(new EntandoCoreComponentUsage("widgets", "my-widget", 1));
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.WIDGET, "my-widget");
        assertThat(cu.getCode()).isEqualTo("my-widget");
        assertThat(cu.getUsage()).isEqualTo(1);
    }

    @Test
    public void shouldReturnIrrelevantUsageInformation() {
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.LABEL, "my-great-label");
        assertThat(cu).isInstanceOf(IrrelevantComponentUsage.class);
        assertThat(cu.getType()).isEqualTo("irrelevant");
        assertThat(cu.getCode()).isEqualTo("my-great-label");
        assertThat(cu.getUsage()).isEqualTo(0);
    }

}
