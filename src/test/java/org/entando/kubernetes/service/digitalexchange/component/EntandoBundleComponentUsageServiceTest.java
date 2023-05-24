package org.entando.kubernetes.service.digitalexchange.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.ComponentReference;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.ComponentReferenceType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("in-process")
class EntandoBundleComponentUsageServiceTest {

    private EntandoBundleComponentUsageService usageService;
    private EntandoCoreClient client;

    @BeforeEach
    public void setup() {
        client = Mockito.mock(EntandoCoreClient.class);
        this.usageService = new EntandoBundleComponentUsageService(client);
    }

    @Test
    void shouldReturnComponentUsageForValidComponent() {
        when(client.getWidgetUsage("my-widget"))
                .thenReturn(new EntandoCoreComponentUsage("widgets", "my-widget", true, 1, Collections.emptyList()));
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.WIDGET, "my-widget");
        assertThat(cu.getCode()).isEqualTo("my-widget");
        assertThat(cu.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldReturnIrrelevantUsageInformation() {
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.LABEL, "my-great-label");
        assertThat(cu).isInstanceOf(IrrelevantComponentUsage.class);
        assertThat(cu.getType()).isEqualTo("irrelevant");
        assertThat(cu.getCode()).isEqualTo("my-great-label");
        assertThat(cu.getUsage()).isZero();
    }

    @Test
    void shouldComponentUsageDetailsReturnCorrectData() {
        final String componentCode = "my-widget";
        when(client.getComponentsUsageDetails(anyList()))
                .thenReturn(Collections.singletonList(
                        new EntandoCoreComponentUsage(ComponentType.WIDGET.getTypeName(), componentCode, true, 1,
                                Arrays.asList(
                                        new ComponentReference(ComponentType.WIDGET.getTypeName(), null, componentCode),
                                        new ComponentReference(ComponentType.PAGE.getTypeName(), null, "page123")))));

        EntandoBundleComponentJobEntity componentJob = new EntandoBundleComponentJobEntity();
        componentJob.setComponentId(componentCode);
        componentJob.setComponentType(ComponentType.WIDGET);

        List<EntandoCoreComponentUsage> cuList = this.usageService.getComponentsUsageDetails(
                Collections.singletonList(componentJob));

        assertThat(cuList).hasSize(1);
        assertThat(cuList.get(0).getReferences()).hasSize(2);
        assertThat(cuList.get(0).getReferences().get(0).getReferenceType()).isEqualTo(ComponentReferenceType.INTERNAL);
        assertThat(cuList.get(0).getReferences().get(1).getReferenceType()).isEqualTo(ComponentReferenceType.EXTERNAL);
    }

}
