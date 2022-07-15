package org.entando.kubernetes.controller.digitalexchange.component;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.service.digitalexchange.job.PostInitService;
import org.entando.kubernetes.service.digitalexchange.job.PostInitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class PostInitResourceControllerTest {

    private PostInitResource controller;
    private PostInitService postInitService;

    @BeforeEach
    public void setup() {
        postInitService = Mockito.mock(PostInitService.class);
        controller = new PostInitResourceController(postInitService);
    }

    @Test
    void getStatus_shouldBeOk() {
        when(postInitService.getStatus()).thenReturn(PostInitStatus.SUCCESSFUL);
        assertThat(controller.getStatus().getBody()).isEqualTo("SUCCESSFUL");
    }

}