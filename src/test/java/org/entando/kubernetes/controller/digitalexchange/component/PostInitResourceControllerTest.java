package org.entando.kubernetes.controller.digitalexchange.component;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.service.digitalexchange.job.PostInitStatus;
import org.entando.kubernetes.service.digitalexchange.job.PostInitStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class PostInitResourceControllerTest {

    private PostInitResource controller;
    private PostInitStatusService postInitStatusService;

    @BeforeEach
    public void setup() {
        postInitStatusService = Mockito.mock(PostInitStatusService.class);
        controller = new PostInitResourceController(postInitStatusService);
    }

    @Test
    void getStatus_shouldBeOk() {
        when(postInitStatusService.getStatus()).thenReturn(PostInitStatus.SUCCESSFUL);
        assertThat(controller.getStatus().getBody()).isEqualTo("SUCCESSFUL");
    }

}