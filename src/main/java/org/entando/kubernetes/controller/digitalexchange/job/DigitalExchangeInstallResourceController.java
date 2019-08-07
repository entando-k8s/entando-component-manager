package org.entando.kubernetes.controller.digitalexchange.job;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangeInstallService;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeInstallResourceController implements DigitalExchangeInstallResource {

    private final @NonNull DigitalExchangeInstallService digitalExchangeInstallService;

    @Override
    public SimpleRestResponse<DigitalExchangeJob> install(
            @PathVariable("exchange") String digitalExchangeId,
            @PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(digitalExchangeInstallService.install(digitalExchangeId, componentId));
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastInstallJob(@PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(digitalExchangeInstallService.getJob(componentId));
    }

    @Override
    public ResponseEntity<SimpleRestResponse<DigitalExchangeJob>> uninstall(
            @PathVariable("component") String componentId,
            HttpServletRequest request) throws URISyntaxException {
        return null;
    }

    @Override
    public ResponseEntity<SimpleRestResponse<DigitalExchangeJob>> getLastUninstallJob(
            @PathVariable("component") String componentId) {
        return null;
    }
}
