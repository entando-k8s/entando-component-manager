package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.exception.job.JobNotFoundException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeInstallService;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeUninstallService;
import org.entando.kubernetes.model.web.exception.HttpException;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
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
        DigitalExchangeJob lastInstallJob = installService.getAllJobs(componentId)
                .stream().filter(j -> JobType.isOfType(j.getStatus(), JobType.INSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);

        return new SimpleRestResponse<>(lastInstallJob);
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> uninstall(
            @PathVariable("component") String componentId, HttpServletRequest request) {
        return new SimpleRestResponse<>(uninstallService.uninstall(componentId));
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastUninstallJob(@PathVariable("component") String componentId) {
        DigitalExchangeJob lastUninstallJob = installService.getAllJobs(componentId)
                .stream()
                .filter(j -> JobType.isOfType(j.getStatus(), JobType.UNINSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);
        return new SimpleRestResponse<>(lastUninstallJob);
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
