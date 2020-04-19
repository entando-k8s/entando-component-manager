package org.entando.kubernetes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponentsController;
import org.entando.kubernetes.exception.web.BadRequestException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

@Tag("in-process")
public class DigitalExchangeComponentControllerTest {

    private DigitalExchangeComponentsController controller;
    private DigitalExchangeComponentsService componentsService;

    @BeforeEach
    public void setup() {
        componentsService = Mockito.mock(DigitalExchangeComponentsService.class);
        controller = new DigitalExchangeComponentsController(componentsService);
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenGettingSummaryOfNotInstalledComponent() {
        when(componentsService.getInstalledComponent(any())).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            controller.getUsageSummary("any");
        });
    }

    @Test
    public void shouldReturnSummaryForInstalledComponent() {
        DigitalExchangeComponent component = getTestComponent();
        when(componentsService.getInstalledComponent(any())).thenReturn(Optional.of(component));

        ResponseEntity response = controller.getUsageSummary("any");
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    private DigitalExchangeComponent getTestComponent() {
        DigitalExchangeComponent component = new DigitalExchangeComponent();
        component.setId("my-component");
        component.setName("my-component-name");
        component.setInstalled(true);
        return component;
    }
}
