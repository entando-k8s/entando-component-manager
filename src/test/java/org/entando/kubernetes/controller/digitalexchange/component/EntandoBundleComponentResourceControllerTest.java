package org.entando.kubernetes.controller.digitalexchange.component;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundleComponentResourceControllerTest {

    private EntandoBundleComponentResourceController controller;

    @Mock
    private EntandoBundleComponentService bundleComponentService;

    @BeforeEach
    public void setup() {
        controller = new EntandoBundleComponentResourceController(bundleComponentService);
    }

    //    @Test
    //    void getBundleByRestNamedId_withRepoUrlNamedParam_shouldReturnABundle() {
    //
    //        String base64EncodedRepoUrl = "aHR0cHM6Ly9naXRodWIuY29tL2ZpcmVnbG92ZXMtYnVuZGxlcy94bWFzYnVuZGxlLmdpdCc";
    //        final RestNamedId id = RestNamedId.of("url",
    //                base64EncodedRepoUrl);
    //        final EntandoBundle bundle = TestEntitiesGenerator.getTestEntandoBundle();
    //        when(
    //                bundleService.getBundleByRepoUrl(base64EncodedRepoUrl)).thenReturn(
    //                Optional.ofNullable(bundle));
    //        final ResponseEntity<SimpleRestResponse<EntandoBundle>> response = controller.getBundleByRestNamedId(id);
    //        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(bundle);
    //    }

    @Test
    void getBundleInstalledComponents_withInvalidResourceId_shouldThrowException() {
        final RestNamedId idWrongFormat = RestNamedId.from("=abc");
        assertThrows(DefaultProblem.class,
                () -> controller.getBundleInstalledComponents(idWrongFormat, new PagedListRequest()));

        final PagedListRequest req = new PagedListRequest();
        String urlEncode = Base64.getEncoder().encodeToString("mccp://-test".getBytes(StandardCharsets.UTF_8));
        when(
                bundleComponentService.getInstalledComponentsByEncodedUrl(req, urlEncode)).thenThrow(
                new EntandoValidationException());
        final RestNamedId idWrongValue = RestNamedId.from(
                EntandoBundleComponentResourceController.REPO_URL_PATH_PARAM + RestNamedId.SEPARATOR + urlEncode);
        assertThrows(EntandoValidationException.class,
                () -> controller.getBundleInstalledComponents(idWrongValue, req));
    }

}
