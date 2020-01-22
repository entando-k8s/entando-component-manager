package org.entando.kubernetes.controller.digitalexchange.job;

import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeInstallService;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeUninstallService;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeInstallResourceController implements DigitalExchangeInstallResource {

    private final @NonNull DigitalExchangeInstallService installService;
    private final @NonNull DigitalExchangeUninstallService uninstallService;

    @Override
    public SimpleRestResponse<DigitalExchangeJob> install(
            @PathVariable("component") String componentId,
            @RequestParam(name = "version", required = true, defaultValue = "latest") String version) {
        return new SimpleRestResponse<>(installService.install(componentId, version));
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
