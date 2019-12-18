package org.entando.kubernetes.controller.digitalexchange.job;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeInstallService;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeUninstallService;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeInstallResourceController implements DigitalExchangeInstallResource {

    private final @NonNull DigitalExchangeInstallService installService;
    private final @NonNull DigitalExchangeUninstallService uninstallService;

    @Override
    public SimpleRestResponse<DigitalExchangeJob> install(
            @PathVariable("exchange") String digitalExchangeId,
            @PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(installService.install(digitalExchangeId, componentId));
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastInstallJob(@PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(installService.getJob(componentId));
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> uninstall(
            @PathVariable("component") String componentId, HttpServletRequest request) {
        return new SimpleRestResponse<>(uninstallService.uninstall(componentId));
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastUninstallJob(@PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(installService.getJob(componentId));
    }
}
