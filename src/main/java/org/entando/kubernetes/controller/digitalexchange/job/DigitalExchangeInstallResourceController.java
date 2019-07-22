package org.entando.kubernetes.controller.digitalexchange.job;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.DigitalExchangeInstallService;
import org.entando.web.response.EntandoEntity;
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
    public EntandoEntity<DigitalExchangeJob> install(
            @PathVariable("exchange") String digitalExchangeId,
            @PathVariable("component") String componentId,
            HttpServletRequest request) {
        return new EntandoEntity<>(digitalExchangeInstallService.install(digitalExchangeId, componentId));
    }

    @Override
    public EntandoEntity<DigitalExchangeJob> getLastInstallJob(@PathVariable("component") String componentId) {
        return new EntandoEntity<>(digitalExchangeInstallService.getJob(componentId));
    }

    @Override
    public ResponseEntity<EntandoEntity<DigitalExchangeJob>> uninstall(
            @PathVariable("component") String componentId,
            HttpServletRequest request) throws URISyntaxException {
        return null;
    }

    @Override
    public ResponseEntity<EntandoEntity<DigitalExchangeJob>> getLastUninstallJob(
            @PathVariable("component") String componentId) {
        return null;
    }
}
