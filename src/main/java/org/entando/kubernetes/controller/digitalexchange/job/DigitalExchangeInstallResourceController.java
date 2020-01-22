package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeInstallService;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeUninstallService;
import org.entando.web.exception.HttpException;
import org.entando.web.response.PagedMetadata;
import org.entando.web.response.PagedRestResponse;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        DigitalExchangeJob installJob;
        try {
            installJob = installService.install(componentId, version);
        } catch (K8SServiceClientException ex) {
            throw new HttpException(HttpStatus.NOT_FOUND, "org.entando.error.bundleNotFound", new Object[] {componentId});
        }
        return new SimpleRestResponse<>(installJob);
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

    @Override
    public ResponseEntity<PagedRestResponse<DigitalExchangeJob>> getComponentJobs(@PathVariable("component") String componentId) {
        List<DigitalExchangeJob> componentJobs = installService.getAllJobs(componentId);
        PagedMetadata<DigitalExchangeJob> pagedMetadata = new PagedMetadata<>();
        pagedMetadata.setBody(componentJobs);
        PagedRestResponse<DigitalExchangeJob> response = new PagedRestResponse(pagedMetadata);
        return ResponseEntity.ok(response);
    }
}
