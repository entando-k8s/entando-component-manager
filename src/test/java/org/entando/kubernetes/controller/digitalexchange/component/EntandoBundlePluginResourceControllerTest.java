package org.entando.kubernetes.controller.digitalexchange.component;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundlePluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundlePluginResourceControllerTest {

    private EntandoBundlePluginResourceController controller;

    @Mock
    private EntandoBundlePluginService bundleComponentService;

    @BeforeEach
    public void setup() {
        controller = new EntandoBundlePluginResourceController(bundleComponentService);
    }

    @Test
    void getBundleInstalledComponents_withValidBundleId_shouldBeOk() {
        final PagedListRequest req = new PagedListRequest();
        when(
                bundleComponentService.getInstalledPluginsByBundleId(eq(req), any())).thenReturn(
                new PagedMetadata(req, new ArrayList<>()));
        final String bundleId = "45355572";
        assertThat(controller.getBundleInstalledComponents(RestNamedId.from(bundleId), req).getBody().getPayload()
                .size()).isEqualTo(0);
    }

    @Test
    void getBundleInstalledComponents_withInvalidResourceId_shouldThrowException() {
        final RestNamedId idWrongFormat = RestNamedId.from("=abc");
        final PagedListRequest request = new PagedListRequest();
        assertThrows(DefaultProblem.class, () -> controller.getBundleInstalledComponents(idWrongFormat, request));

        final RestNamedId idWrongEncode = RestNamedId.from(
                EntandoBundlePluginResourceController.REPO_URL_PATH_PARAM + RestNamedId.SEPARATOR + "09)°");
        assertThrows(DefaultProblem.class, () -> controller.getBundleInstalledComponents(idWrongEncode, request));

        String urlEncode = Base64.getEncoder().encodeToString("mccp://-test".getBytes(StandardCharsets.UTF_8));
        when(
                bundleComponentService.getInstalledPluginsByEncodedUrl(request, urlEncode)).thenThrow(
                new EntandoValidationException());
        final RestNamedId idWrongValue = RestNamedId.from(
                EntandoBundlePluginResourceController.REPO_URL_PATH_PARAM + RestNamedId.SEPARATOR + urlEncode);
        assertThrows(EntandoValidationException.class,
                () -> controller.getBundleInstalledComponents(idWrongValue, request));
    }

    @Test
    void getBundleInstalledPlugin_withValidBundleId_shouldBeOk() {
        final String id = "45355572";
        final RestNamedId bundleId = RestNamedId.from(id);
        final String pluginCode = "45355572";
        final PluginData plugin = new PluginData();
        when(bundleComponentService.getInstalledPlugin(id, pluginCode)).thenReturn(plugin);

        assertThat(controller.getBundleInstalledPlugin(bundleId, pluginCode).getBody()).isEqualTo(plugin);
    }

    @Test
    void getBundleInstalledPlugin_withInvalidResourceId_shouldThrowException() {
        final RestNamedId idWrongFormat = RestNamedId.from("=abc");
        final String pluginCode = "45355572";
        assertThrows(DefaultProblem.class,
                () -> controller.getBundleInstalledPlugin(idWrongFormat, pluginCode));

        final RestNamedId idWrongEncode = RestNamedId.from(
                EntandoBundlePluginResourceController.REPO_URL_PATH_PARAM + RestNamedId.SEPARATOR + "09)°");
        assertThrows(DefaultProblem.class,
                () -> controller.getBundleInstalledPlugin(idWrongEncode, pluginCode));

        String urlEncode = Base64.getEncoder().encodeToString("mccp://-test".getBytes(StandardCharsets.UTF_8));
        when(
                bundleComponentService.getInstalledPluginByEncodedUrl(urlEncode, pluginCode))
                .thenThrow(new EntandoValidationException());
        final RestNamedId idWrongValue = RestNamedId.from(
                EntandoBundlePluginResourceController.REPO_URL_PATH_PARAM + RestNamedId.SEPARATOR + urlEncode);
        assertThrows(EntandoValidationException.class,
                () -> controller.getBundleInstalledPlugin(idWrongValue, pluginCode));
    }
}