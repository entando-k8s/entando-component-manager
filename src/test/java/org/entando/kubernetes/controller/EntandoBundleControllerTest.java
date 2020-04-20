package org.entando.kubernetes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.entando.kubernetes.controller.digitalexchange.component.EntandoBundleResourceController;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

@Tag("in-process")
public class EntandoBundleControllerTest {

    private EntandoBundleResourceController controller;
    private EntandoBundleService componentsService;

    @BeforeEach
    public void setup() {
        componentsService = Mockito.mock(EntandoBundleService.class);
        controller = new EntandoBundleResourceController(componentsService);
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenGettingSummaryOfNotInstalledComponent() {
        when(componentsService.getInstalledComponent(any())).thenReturn(Optional.empty());
        assertThrows(BundleNotInstalledException.class, () -> {
            controller.getBundleUsageSummary("any");
        });
    }

    @Test
    public void shouldReturnSummaryForInstalledComponent() {
        EntandoBundle component = getTestComponent();
        when(componentsService.getInstalledComponent(any())).thenReturn(Optional.of(component));

        ResponseEntity response = controller.getBundleUsageSummary("any");
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    private EntandoBundle getTestComponent() {
        EntandoBundle component = new EntandoBundle();
        component.setId("my-component");
        component.setName("my-component-name");
        component.setInstalled(true);
        return component;
    }
}
